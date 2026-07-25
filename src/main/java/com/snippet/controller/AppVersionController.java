package com.snippet.controller;

import com.snippet.dto.AppVersionDto;
import com.snippet.service.AppVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 앱 버전 정책 조회 — 강제 업데이트 게이트용.
 *
 * <p>로그인 전에도 호출되므로 인증 없이 접근 가능해야 한다 (SecurityConfig permitAll).
 */
@RestController
@RequestMapping("/api/appversion")
@RequiredArgsConstructor
public class AppVersionController {

    private final AppVersionService appVersionService;

    /**
     * GET /api/appversion?platform=ios&version=1.0.28
     *
     * <p>파라미터가 없거나 알 수 없는 값이면 "업데이트 불필요"로 응답한다 (fail-open).
     */
    @GetMapping
    public ResponseEntity<AppVersionDto> check(
            @RequestParam(value = "platform", required = false) String platform,
            @RequestParam(value = "version", required = false) String version) {
        return ResponseEntity.ok(appVersionService.check(platform, version));
    }
}
