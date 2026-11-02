package com.snippet.exception;

/**
 * 외부 도서 검색 API 장애 → 502.
 * 빈 목록으로 숨기면 "검색 결과 없음"과 구분되지 않아, 알라딘 OpenAPI 종료(2026-10-30)를
 * 사용자가 책 추가가 안 된다고 알려줄 때까지 아무도 몰랐다.
 */
public class BookSearchUnavailableException extends RuntimeException {

    public BookSearchUnavailableException() {
        super("도서 검색 서비스에 일시적인 문제가 있습니다. 잠시 후 다시 시도해주세요.");
    }
}
