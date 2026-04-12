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
    public ResponseEntity<AuthDto.RegisterResponse> register(@RequestBody AuthDto.RegisterRequest request) {
        AuthDto.RegisterResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse> login(@RequestBody AuthDto.LoginRequest request) {
        AuthDto.AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sendcode")
    public ResponseEntity<Void> sendCode(@RequestBody AuthDto.SendCodeRequest request) {
        authService.sendVerificationCode(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verifycode")
    public ResponseEntity<AuthDto.AuthResponse> verifyCode(@RequestBody AuthDto.VerifyCodeRequest request) {
        AuthDto.AuthResponse response = authService.verifyCode(request);
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
