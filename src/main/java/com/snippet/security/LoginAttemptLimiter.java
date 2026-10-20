package com.snippet.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로그인 실패 횟수 제한 (이메일+IP 단위).
 * 윈도우(15분) 안에 5회 실패하면 15분간 해당 이메일+IP 조합의 로그인을 차단한다.
 *
 * ⚠️ 인메모리 저장소 — 서버가 단일 인스턴스라는 전제. 재시작 시 카운트가 초기화되고,
 * 인스턴스를 여러 대로 늘리면 Redis 등 공유 저장소로 옮겨야 한다.
 */
@Component
public class LoginAttemptLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private record Attempt(int failures, Instant windowStart, Instant lockedUntil) {}

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    /** 잠겨 있으면 true */
    public boolean isLocked(String email, String ip) {
        Attempt a = attempts.get(key(email, ip));
        return a != null && a.lockedUntil() != null && Instant.now().isBefore(a.lockedUntil());
    }

    public void recordFailure(String email, String ip) {
        Instant now = Instant.now();
        attempts.compute(key(email, ip), (k, a) -> {
            if (a == null || isStale(a, now)) {
                return new Attempt(1, now, null);
            }
            int failures = a.failures() + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK_DURATION) : a.lockedUntil();
            return new Attempt(failures, a.windowStart(), lockedUntil);
        });
    }

    public void recordSuccess(String email, String ip) {
        attempts.remove(key(email, ip));
    }

    /** 만료된 항목 정리 — 무작위 이메일로 맵을 키우는 공격 대비 */
    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void evictExpired() {
        Instant now = Instant.now();
        attempts.entrySet().removeIf(e -> isStale(e.getValue(), now));
    }

    /** 윈도우가 지났고 잠금도 풀린 항목 */
    private static boolean isStale(Attempt a, Instant now) {
        boolean windowOver = now.isAfter(a.windowStart().plus(WINDOW));
        boolean unlocked = a.lockedUntil() == null || !now.isBefore(a.lockedUntil());
        return windowOver && unlocked;
    }

    private static String key(String email, String ip) {
        String e = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return e + "|" + (ip == null ? "" : ip);
    }
}
