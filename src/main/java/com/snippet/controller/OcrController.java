package com.snippet.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snippet.dto.OcrRegionDto;
import com.snippet.dto.OcrResponseDto;
import com.snippet.service.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;
    private final ObjectMapper objectMapper;

    /**
     * 이미지에서 텍스트 추출 (OCR)
     * POST /api/ocr/extract
     * Content-Type: multipart/form-data
     *
     * @param regions 선택적 JSON 배열 - 인식할 영역 [{left,top,right,bottom}] (이미지 픽셀 좌표)
     *                제공 시 해당 영역에 속하는 텍스트만 반환, 미제공 시 전체 텍스트 반환
     */
    @PostMapping("/extract")
    public ResponseEntity<OcrResponseDto> extractText(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "engine", required = false, defaultValue = "google") String engine,
            @RequestParam(value = "regions", required = false) String regionsJson) {

        try {
            List<OcrRegionDto> regions = null;
            if (StringUtils.hasText(regionsJson)) {
                regions = objectMapper.readValue(regionsJson, new TypeReference<List<OcrRegionDto>>() {});
            }
            OcrResponseDto response = ocrService.extractText(image, engine, regions);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("OCR extraction failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
