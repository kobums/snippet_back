package com.snippet.exception;

/** 로그인·인증코드 시도/발송 횟수 초과 → 429 */
public class TooManyAttemptsException extends RuntimeException {

    /** Retry-After 헤더 값(초). 0 이하면 헤더를 붙이지 않는다. */
    private final long retryAfterSeconds;

    public TooManyAttemptsException(String message) {
        this(message, 0);
    }

    public TooManyAttemptsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
