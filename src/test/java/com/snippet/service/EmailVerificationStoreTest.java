package com.snippet.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailVerificationStoreTest {

    @Test
    void correctCodeVerifies() {
        EmailVerificationStore store = new EmailVerificationStore();
        store.save("a@b.c", "123456", 10);
        assertFalse(store.verify("a@b.c", "000000"));
        assertTrue(store.verify("A@b.c", "123456"));
    }

    @Test
    void codeIsDiscardedAfterMaxFailedAttempts() {
        EmailVerificationStore store = new EmailVerificationStore();
        store.save("a@b.c", "123456", 10);
        for (int i = 0; i < EmailVerificationStore.MAX_FAILED_ATTEMPTS; i++) {
            assertFalse(store.verify("a@b.c", "000000"));
        }
        // 폐기된 뒤에는 올바른 코드도 실패
        assertFalse(store.verify("a@b.c", "123456"));
    }

    @Test
    void nullCodeCountsAsFailure() {
        EmailVerificationStore store = new EmailVerificationStore();
        store.save("a@b.c", "123456", 10);
        assertFalse(store.verify("a@b.c", null));
    }
}
