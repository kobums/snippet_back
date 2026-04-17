package com.snippet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // CORS는 SecurityConfig.corsConfigurationSource()에서 환경변수(CORS_ALLOWED_ORIGINS)로 관리
}
