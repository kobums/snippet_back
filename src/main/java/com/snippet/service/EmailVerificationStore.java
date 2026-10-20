package com.snippet.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 가입용 이메일 인증코드 저장소.
 * 6자리 코드는 경우의 수가 100만뿐이라, 오답을 MAX_FAILED_ATTEMPTS회 입력하면 코드를 폐기해
 * 무차별 대입을 막는다 (재발송으로 새 코드를 받아야 함).
 *
 * ⚠️ 인메모리 저장소 — 서버가 단일 인스턴스라는 전제. 여러 대로 늘리면 공유 저장소 필요.
 */
@Component
public class EmailVerificationStore {

    static final int MAX_FAILED_ATTEMPTS = 5;

    private record CodeEntry(String code, LocalDateTime expiresAt, int failedAttempts) {}

    private final ConcurrentHashMap<String, CodeEntry> store = new ConcurrentHashMap<>();

    public void save(String email, String code, int expiryMinutes) {
        store.put(email.toLowerCase(), new CodeEntry(code, LocalDateTime.now().plusMinutes(expiryMinutes), 0));
    }

    public boolean verify(String email, String code) {
        boolean[] matched = {false};
        // compute로 원자적으로 판정·카운트해 동시 요청으로 시도 횟수를 우회하지 못하게 한다.
        store.computeIfPresent(email.toLowerCase(), (k, entry) -> {
            if (LocalDateTime.now().isAfter(entry.expiresAt())) {
                return null; // 만료 → 폐기
            }
            if (code != null && MessageDigest.isEqual(
                    entry.code().getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8))) {
                matched[0] = true;
                return entry;
            }
            int failed = entry.failedAttempts() + 1;
            return failed >= MAX_FAILED_ATTEMPTS
                    ? null // 시도 횟수 초과 → 폐기
                    : new CodeEntry(entry.code(), entry.expiresAt(), failed);
        });
        return matched[0];
    }

    public void remove(String email) {
        store.remove(email.toLowerCase());
    }
}
