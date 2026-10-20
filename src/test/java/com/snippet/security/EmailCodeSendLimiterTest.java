package com.snippet.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailCodeSendLimiterTest {

    /** 테스트에서 시간을 직접 움직이는 시계 */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-20T00:00:00Z");

        void advance(Duration d) { now = now.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void enforcesCooldownPerEmail() {
        MutableClock clock = new MutableClock();
        EmailCodeSendLimiter limiter = new EmailCodeSendLimiter(clock);

        assertNull(limiter.tryAcquire("a@b.c", "1.1.1.1"));
        EmailCodeSendLimiter.Rejection r = limiter.tryAcquire("A@B.c ", "2.2.2.2");
        assertNotNull(r);
        assertEquals(60, r.retryAfterSeconds());

        // 다른 이메일은 영향 없음
        assertNull(limiter.tryAcquire("other@b.c", "1.1.1.1"));

        clock.advance(EmailCodeSendLimiter.COOLDOWN);
        assertNull(limiter.tryAcquire("a@b.c", "1.1.1.1"));
    }

    @Test
    void limitsSendsPerEmailPerHour() {
        MutableClock clock = new MutableClock();
        EmailCodeSendLimiter limiter = new EmailCodeSendLimiter(clock);

        for (int i = 0; i < EmailCodeSendLimiter.MAX_PER_EMAIL; i++) {
            assertNull(limiter.tryAcquire("a@b.c", "ip" + i));
            clock.advance(EmailCodeSendLimiter.COOLDOWN);
        }
        EmailCodeSendLimiter.Rejection r = limiter.tryAcquire("a@b.c", "fresh-ip");
        assertNotNull(r);
        assertTrue(r.retryAfterSeconds() > 0);

        // 가장 오래된 발송이 1시간 창을 벗어나면 다시 허용
        clock.advance(EmailCodeSendLimiter.WINDOW);
        assertNull(limiter.tryAcquire("a@b.c", "fresh-ip"));
    }

    @Test
    void limitsSendsPerIpPerHour() {
        MutableClock clock = new MutableClock();
        EmailCodeSendLimiter limiter = new EmailCodeSendLimiter(clock);

        for (int i = 0; i < EmailCodeSendLimiter.MAX_PER_IP; i++) {
            assertNull(limiter.tryAcquire("user" + i + "@b.c", "1.1.1.1"));
        }
        assertNotNull(limiter.tryAcquire("new@b.c", "1.1.1.1"));
        // 거절된 요청은 기록되지 않아 이메일 쿨다운이 걸리지 않는다
        assertNull(limiter.tryAcquire("new@b.c", "2.2.2.2"));
    }

    @Test
    void evictionDropsExpiredEntries() {
        MutableClock clock = new MutableClock();
        EmailCodeSendLimiter limiter = new EmailCodeSendLimiter(clock);
        assertNull(limiter.tryAcquire("a@b.c", "1.1.1.1"));
        clock.advance(EmailCodeSendLimiter.WINDOW.plusSeconds(1));
        limiter.evictExpired();
        assertNull(limiter.tryAcquire("a@b.c", "1.1.1.1"));
    }
}
