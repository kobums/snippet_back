package com.snippet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OcrRequestDto {
    @NotBlank(message = "이미지 데이터는 필수입니다")
    private String imageBase64; // Base64 인코딩된 이미지

    private String engine; // OCR 엔진: "google" 또는 "naver" (기본값: google)
}
