package com.snippet.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 관리자 판별 — 역할(role) 컬럼 없이 ADMIN_EMAILS 환경변수(콤마 구분)로 관리자 이메일을 지정한다.
 * 미설정 시 모든 요청을 거부한다.
 */
@Component
public class AdminGuard {

    private final List<String> adminEmails;

    public AdminGuard(@Value("${ADMIN_EMAILS:}") String adminEmails) {
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();
    }

    public boolean isAdmin(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser().getEmail() == null) return false;
        // 가입 시 입력한 대소문자와 ADMIN_EMAILS 표기가 달라도 매칭되도록 소문자로 비교
        return adminEmails.contains(userDetails.getUser().getEmail().toLowerCase(Locale.ROOT));
    }

    public void check(CustomUserDetails userDetails) {
        if (!isAdmin(userDetails)) {
            throw new AccessDeniedException("관리자 권한이 필요합니다.");
        }
    }
}
