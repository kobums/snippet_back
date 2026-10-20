package com.snippet.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptLimiterTest {

    @Test
    void locksAfterMaxFailuresPerEmailAndIp() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter();
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES - 1; i++) {
            limiter.recordFailure("User@Example.com", "1.1.1.1");
        }
        assertFalse(limiter.isLocked("user@example.com", "1.1.1.1"));

        limiter.recordFailure("user@example.com", "1.1.1.1");
        assertTrue(limiter.isLocked("USER@example.com", "1.1.1.1"));
        // 다른 IP는 영향 없음
        assertFalse(limiter.isLocked("user@example.com", "2.2.2.2"));
    }

    @Test
    void successResetsFailures() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter();
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES - 1; i++) {
            limiter.recordFailure("a@b.c", "1.1.1.1");
        }
        limiter.recordSuccess("a@b.c", "1.1.1.1");
        limiter.recordFailure("a@b.c", "1.1.1.1");
        assertFalse(limiter.isLocked("a@b.c", "1.1.1.1"));
    }
}
