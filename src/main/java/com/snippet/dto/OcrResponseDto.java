package com.snippet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OcrResponseDto {
    private String extractedText;
    private int confidence; // 신뢰도 (0-100)
}
