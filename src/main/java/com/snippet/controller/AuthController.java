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

    /** 가입 전 이메일 인증코드 발송 */
    @PostMapping("/emailcode")
    public ResponseEntity<Void> sendEmailCode(@RequestBody AuthDto.EmailCodeRequest request) {
        authService.sendEmailCode(request);
        return ResponseEntity.ok().build();
    }

    /** 인증코드 검증 후 회원가입 (토큰 즉시 반환) */
    @PostMapping("/register")
    public ResponseEntity<AuthDto.AuthResponse> register(@RequestBody AuthDto.RegisterRequest request) {
        AuthDto.AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse> login(@RequestBody AuthDto.LoginRequest request) {
        AuthDto.AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDto.AuthResponse> me(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(authService.getMe(token));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDto.RefreshResponse> refresh(@RequestBody AuthDto.RefreshRequest request) {
        AuthDto.RefreshResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        // /api/auth/** 는 permitAll 이므로 여기서 직접 검증해야 한다.
        // 검증 없이 파싱하면 만료·위조 토큰이 파싱 예외로 500을 내며, 클라이언트는
        // "탈퇴 실패"만 보고 재로그인해야 한다는 걸 알 수 없다.
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        String email = jwtTokenProvider.getUserEmailFromToken(token);
        authService.deleteAccount(email);
        return ResponseEntity.ok().build();
    }
}
