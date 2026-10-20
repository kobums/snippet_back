package com.snippet.exception;

/** 로그인·인증코드 시도 횟수 초과 → 429 */
public class TooManyAttemptsException extends RuntimeException {
    public TooManyAttemptsException(String message) {
        super(message);
    }
}
