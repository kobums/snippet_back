package com.snippet.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        private String email;
        private String password;
        private String name;
    }

    @Getter
    @AllArgsConstructor
    public static class RegisterResponse {
        private String email;
        private String message;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Getter
    @AllArgsConstructor
    public static class AuthResponse {
        private Long id;
        private String email;
        private String name;
        private String token;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendCodeRequest {
        private String email;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyCodeRequest {
        private String email;
        private String code;
    }
}
