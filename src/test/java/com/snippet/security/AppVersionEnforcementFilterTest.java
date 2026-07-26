package com.snippet.security;

import com.snippet.service.AppVersionService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 강제 업데이트 서버측 백스톱 필터 테스트.
 *
 * <p>웹 브라우저·정상 버전 앱이 차단되는 사고가 최악이므로,
 * "절대 차단되면 안 되는 경우"를 차단 케이스보다 촘촘히 검증한다.
 */
class AppVersionEnforcementFilterTest {

    private static final String LEGACY_IOS_UA =
            "Snippet/42 CFNetwork/1568.100.1 Darwin/24.0.0";
    private static final String BROWSER_UA =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36";

    private AppVersionEnforcementFilter filter;
    private AppVersionService service;

    @BeforeEach
    void setUp() {
        service = new AppVersionService();
        setPolicy("1.0.29", "https://apps.apple.com/kr/app/id6759643636");
        filter = new AppVersionEnforcementFilter(service);
    }

    private void setPolicy(String iosMin, String iosStore) {
        ReflectionTestUtils.setField(service, "iosMinVersion", iosMin);
        ReflectionTestUtils.setField(service, "iosLatestVersion", iosMin);
        ReflectionTestUtils.setField(service, "iosStoreUrl", iosStore);
        ReflectionTestUtils.setField(service, "androidMinVersion", "");
        ReflectionTestUtils.setField(service, "androidLatestVersion", "");
        ReflectionTestUtils.setField(service, "androidStoreUrl", "");
        ReflectionTestUtils.setField(service, "updateMessage", "");
    }

    private MockHttpServletResponse run(MockHttpServletRequest request)
            throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", uri);
        req.setRequestURI(uri);
        return req;
    }

    // ─── 차단되면 안 되는 경우 (fail-open) ───────────────────────

    @Test
    @DisplayName("웹 브라우저(UA Mozilla, 헤더 없음)는 통과한다")
    void browserPasses() throws Exception {
        MockHttpServletRequest req = request("/api/records");
        req.addHeader("User-Agent", BROWSER_UA);
        assertThat(run(req).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("UA 자체가 없는 요청(curl 등)은 통과한다")
    void noUserAgentPasses() throws Exception {
        assertThat(run(request("/api/records")).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("게이트 포함 iOS(min 충족)는 통과한다")
    void currentIosPasses() throws Exception {
        MockHttpServletRequest req = request("/api/records");
        req.addHeader("X-App-Platform", "ios");
        req.addHeader("X-App-Version", "1.0.29");
        assertThat(run(req).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Android는 min 미설정이므로 어떤 버전이든 통과한다")
    void androidPassesWhenPolicyDisabled() throws Exception {
        MockHttpServletRequest req = request("/api/records");
        req.addHeader("X-App-Platform", "android");
        req.addHeader("X-App-Version", "1.0.17");
        assertThat(run(req).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("iOS min이 미설정이면 레거시 iOS(CFNetwork)도 통과한다")
    void legacyIosPassesWhenPolicyDisabled() throws Exception {
        setPolicy("", "");
        MockHttpServletRequest req = request("/api/records");
        req.addHeader("User-Agent", LEGACY_IOS_UA);
        assertThat(run(req).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("스토어 URL이 없으면 강제 차단하지 않는다")
    void legacyIosPassesWhenStoreUrlMissing() throws Exception {
        setPolicy("1.0.29", "");
        MockHttpServletRequest req = request("/api/records");
        req.addHeader("User-Agent", LEGACY_IOS_UA);
        assertThat(run(req).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("/api/appversion은 레거시 클라이언트도 항상 허용한다")
    void appVersionEndpointAlwaysPasses() throws Exception {
        MockHttpServletRequest req = request("/api/appversion");
        req.addHeader("User-Agent", LEGACY_IOS_UA);
        assertThat(run(req).getStatus()).isEqualTo(200);
    }

    // ─── 차단되어야 하는 경우 ────────────────────────────────────

    @Test
    @DisplayName("레거시 iOS(헤더 없음 + CFNetwork UA)는 426으로 차단된다")
    void legacyIosBlocked() throws Exception {
        MockHttpServletRequest req = request("/api/records");
        req.addHeader("User-Agent", LEGACY_IOS_UA);
        MockHttpServletResponse res = run(req);
        assertThat(res.getStatus()).isEqualTo(426);
        assertThat(res.getContentAsString()).contains("storeUrl").contains("apps.apple.com");
    }

    @Test
    @DisplayName("게이트 포함 iOS라도 min 미만 버전이면 서버가 426으로 막는다")
    void headerClientBelowMinBlocked() throws Exception {
        MockHttpServletRequest req = request("/api/records");
        req.addHeader("X-App-Platform", "ios");
        req.addHeader("X-App-Version", "1.0.28");
        assertThat(run(req).getStatus()).isEqualTo(426);
    }
}
