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

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtTokenProvider.getUserEmailFromToken(token);
        authService.deleteAccount(email);
        return ResponseEntity.ok().build();
    }
}
