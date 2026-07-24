package com.snippet.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

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
                .toList();
    }

    public boolean isAdmin(CustomUserDetails userDetails) {
        return userDetails != null && adminEmails.contains(userDetails.getUser().getEmail());
    }

    public void check(CustomUserDetails userDetails) {
        if (!isAdmin(userDetails)) {
            throw new AccessDeniedException("관리자 권한이 필요합니다.");
        }
    }
}
