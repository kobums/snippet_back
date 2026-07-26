package com.snippet.security;

import com.snippet.dto.AppVersionDto;
import com.snippet.service.AppVersionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 강제 업데이트 서버측 백스톱.
 *
 * <p>클라이언트 게이트(/api/appversion 조회 후 자체 차단)는 게이트 코드가 들어있는
 * 버전(iOS 1.0.29+, Android 네이티브)부터만 동작한다. 그 이전 버전은 조회 자체를
 * 하지 않으므로, 서버가 요청 단위로 구버전을 식별해 426으로 거부해야만 강제할 수 있다.
 *
 * <p>식별 규칙:
 * <ul>
 *   <li>X-App-Platform/X-App-Version 헤더가 있으면(게이트 포함 버전) 기존 정책 판정을
 *       그대로 적용 — min 미달이면 426. 게이트를 우회한 클라이언트도 서버가 막는다.</li>
 *   <li>헤더가 없는데 User-Agent에 CFNetwork가 있으면 게이트 이전 iOS 앱이다
 *       (URLSession 기본 UA). iOS 강제 업데이트가 활성일 때만 426.</li>
 *   <li>그 외(브라우저 Mozilla, curl 등)는 전부 통과 — 웹 프론트는 영향 없다.</li>
 * </ul>
 *
 * <p>fail-open: 정책 미설정 시 아무도 차단하지 않으며, /api/appversion은 항상 허용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppVersionEnforcementFilter extends OncePerRequestFilter {

    private static final String HEADER_PLATFORM = "X-App-Platform";
    private static final String HEADER_VERSION = "X-App-Version";

    private final AppVersionService appVersionService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 버전 정책 조회는 항상 허용 (게이트 포함 버전이 정책을 읽는 통로)
        return request.getRequestURI().startsWith("/api/appversion");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String platform = request.getHeader(HEADER_PLATFORM);
        String version = request.getHeader(HEADER_VERSION);

        AppVersionDto verdict = null;

        if (platform != null && version != null) {
            // 게이트 포함 클라이언트 — 서버가 한 번 더 강제 (게이트 우회 방지)
            verdict = appVersionService.check(platform, version);
        } else {
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null && userAgent.contains("CFNetwork")) {
                // 게이트 이전 iOS 앱: 버전 헤더가 없으므로 min보다 낮다고 간주한다.
                // (버전 헤더는 게이트와 같은 릴리즈(1.0.29)에 추가됨)
                verdict = appVersionService.check("ios", "0.0.1");
            }
        }

        if (verdict != null && verdict.isUpdateRequired()) {
            log.info("구버전 클라이언트 차단: platform={}, version={}, ua={}",
                    platform, version, request.getHeader("User-Agent"));
            writeUpgradeRequired(response, verdict);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUpgradeRequired(HttpServletResponse response, AppVersionDto verdict) throws IOException {
        response.setStatus(426); // Upgrade Required
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String message = verdict.getMessage() != null
                ? verdict.getMessage()
                : "새로운 버전이 출시되었습니다. 스토어에서 앱을 업데이트해 주세요.";
        String body = String.format(
                "{\"message\":\"%s\",\"storeUrl\":\"%s\",\"minVersion\":\"%s\"}",
                message,
                verdict.getStoreUrl() == null ? "" : verdict.getStoreUrl(),
                verdict.getMinVersion());
        response.getWriter().write(body);
    }
}
