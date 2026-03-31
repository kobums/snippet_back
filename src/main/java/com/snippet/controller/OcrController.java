package com.snippet.controller;

import com.snippet.dto.OcrResponseDto;
import com.snippet.service.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    /**
     * 이미지에서 텍스트 추출 (OCR)
     * POST /api/ocr/extract
     * Content-Type: multipart/form-data
     */
    @PostMapping("/extract")
    public ResponseEntity<OcrResponseDto> extractText(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "engine", required = false, defaultValue = "google") String engine) {

        log.info("📝 [OCR] Received OCR request (engine: {}, file: {})", engine, image.getOriginalFilename());

        try {
            OcrResponseDto response = ocrService.extractText(image, engine);
            log.info("✅ [OCR] Text extraction successful");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("❌ [OCR] Failed to extract text: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
