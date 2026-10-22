package com.snippet.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JWT 필터 테스트. 토큰 subject 가 이메일이라 이메일 변경·회원탈퇴 직후엔 서명은 유효한데
 * 계정이 없는 토큰이 최대 1시간 남는다 — 이때 500 이 아니라 401 로 이어져야 클라이언트가 refresh 한다.
 */
class JwtAuthenticationFilterTest {

    private final JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
    private final CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/userbooks");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    @Test
    @DisplayName("토큰 속 이메일의 계정이 없으면 예외 없이 인증 없이 통과한다 (보호 경로에서 401)")
    void missingUserPassesThroughUnauthenticated() throws Exception {
        when(tokenProvider.validateToken("stale")).thenReturn(true);
        when(tokenProvider.getUserEmailFromToken("stale")).thenReturn("old@example.com");
        when(userDetailsService.loadUserByUsername("old@example.com"))
                .thenThrow(new UsernameNotFoundException("사용자를 찾을 수 없습니다: old@example.com"));

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(requestWithToken("stale"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).as("다음 필터로 넘어가야 한다").isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("계정이 있으면 인증을 세운다")
    void existingUserIsAuthenticated() throws Exception {
        UserDetails user = mock(UserDetails.class);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        when(tokenProvider.validateToken("ok")).thenReturn(true);
        when(tokenProvider.getUserEmailFromToken("ok")).thenReturn("me@example.com");
        when(userDetailsService.loadUserByUsername("me@example.com")).thenReturn(user);
        when(tokenProvider.getAuthentication("ok", user)).thenReturn(auth);

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(requestWithToken("ok"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(auth);
    }
}
