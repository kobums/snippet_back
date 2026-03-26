package com.snippet.controller;

import com.snippet.dto.auth.AuthDto;
import com.snippet.security.JwtTokenProvider;
import com.snippet.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<AuthDto.AuthResponse> register(@RequestBody AuthDto.RegisterRequest request) {
        AuthDto.AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse> login(@RequestBody AuthDto.LoginRequest request) {
        AuthDto.AuthResponse response = authService.login(request);
        // 향후 JWT 발급 로직 연동
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@RequestHeader("Authorization") String authHeader) {
        // Bearer 토큰에서 실제 토큰 추출
        String token = authHeader.replace("Bearer ", "");

        // 토큰에서 사용자 이메일 추출
        String email = jwtTokenProvider.getUserEmailFromToken(token);

        // 회원 탈퇴 처리
        authService.deleteAccount(email);

        return ResponseEntity.ok().build();
    }
}
