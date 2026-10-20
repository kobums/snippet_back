package com.snippet.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 이메일 인증코드 발송 빈도 제한.
 * - 같은 이메일: 직전 발송 후 60초 쿨다운, 1시간에 최대 5회
 * - 같은 IP: 1시간에 최대 10회 (통신사 NAT·사무실처럼 IP를 공유하는 경우를 고려해 이메일보다 느슨하게)
 *
 * 메일 폭탄(타인 메일함에 코드 연타)과 SMTP 발송 한도 소진을 막는다.
 * 가입 여부와 무관하게 모든 요청에 똑같이 적용해 응답 차이로 가입 여부가 드러나지 않게 한다.
 *
 * ⚠️ 인메모리 저장소 — 서버가 단일 인스턴스라는 전제. 여러 대로 늘리면 Redis 등 공유 저장소 필요.
 */
@Component
public class EmailCodeSendLimiter {

    static final Duration COOLDOWN = Duration.ofSeconds(60);
    static final Duration WINDOW = Duration.ofHours(1);
    static final int MAX_PER_EMAIL = 5;
    static final int MAX_PER_IP = 10;

    /** 거절 사유와 재시도까지 남은 초 */
    public record Rejection(String message, long retryAfterSeconds) {}

    private final Map<String, Deque<Instant>> byEmail = new HashMap<>();
    private final Map<String, Deque<Instant>> byIp = new HashMap<>();
    private final Clock clock;

    public EmailCodeSendLimiter() {
        this(Clock.systemUTC());
    }

    EmailCodeSendLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * 발송 가능하면 발송 1회를 기록하고 null, 아니면 거절 사유를 반환한다.
     * 이메일·IP 두 조건을 함께 확인한 뒤 기록해야 하므로 메서드 단위로 동기화한다(트래픽이 작은 엔드포인트).
     */
    public synchronized Rejection tryAcquire(String email, String ip) {
        Instant now = clock.instant();
        Deque<Instant> emailHits = prune(byEmail.computeIfAbsent(normalize(email), k -> new ArrayDeque<>()), now);
        Deque<Instant> ipHits = prune(byIp.computeIfAbsent(ip == null ? "" : ip, k -> new ArrayDeque<>()), now);

        if (!emailHits.isEmpty()) {
            Instant cooldownEnd = emailHits.peekLast().plus(COOLDOWN);
            if (now.isBefore(cooldownEnd)) {
                long wait = Math.max(1, Duration.between(now, cooldownEnd).toSeconds());
                return new Rejection("인증 코드는 " + wait + "초 후에 다시 요청할 수 있습니다.", wait);
            }
        }
        if (emailHits.size() >= MAX_PER_EMAIL) {
            return tooMany(emailHits, now);
        }
        if (ipHits.size() >= MAX_PER_IP) {
            return tooMany(ipHits, now);
        }

        emailHits.addLast(now);
        ipHits.addLast(now);
        return null;
    }

    /** 오래된 기록 정리 — 무작위 이메일로 맵을 키우는 공격 대비 */
    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public synchronized void evictExpired() {
        Instant now = clock.instant();
        byEmail.values().removeIf(d -> prune(d, now).isEmpty());
        byIp.values().removeIf(d -> prune(d, now).isEmpty());
    }

    private static Rejection tooMany(Deque<Instant> hits, Instant now) {
        Instant oldestExpiry = hits.peekFirst().plus(WINDOW);
        long wait = Math.max(1, Duration.between(now, oldestExpiry).toSeconds());
        return new Rejection("인증 코드 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.", wait);
    }

    private static Deque<Instant> prune(Deque<Instant> hits, Instant now) {
        Instant cutoff = now.minus(WINDOW);
        while (!hits.isEmpty() && !hits.peekFirst().isAfter(cutoff)) {
            hits.pollFirst();
        }
        return hits;
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
