package com.snippet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.snippet.dto.OcrResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class OcrService {

    @Value("${naver.clova.api-url:}")
    private String naverClovaApiUrl;

    @Value("${naver.clova.secret-key:}")
    private String naverClovaSecretKey;

    @Value("${google.application.credentials:}")
    private String googleCredentialsPath;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * OCR 엔진을 선택하여 텍스트 추출
     */
    public OcrResponseDto extractText(MultipartFile imageFile, String engine) throws IOException {
        long startTime = System.currentTimeMillis();

        // 이미지 크기 로깅
        long fileSize = imageFile.getSize();
        log.info("📸 [OCR] Received image: {} bytes ({} KB)", fileSize, fileSize / 1024);

        OcrResponseDto response;
        if (engine == null || engine.isEmpty() || "google".equalsIgnoreCase(engine)) {
            log.info("🔍 [OCR] Using Google Cloud Vision");
            response = extractTextWithGoogle(imageFile);
        } else if ("naver".equalsIgnoreCase(engine)) {
            log.info("🔍 [OCR] Using Naver Clova OCR");
            response = extractTextWithNaver(imageFile);
        } else {
            throw new IllegalArgumentException("Unsupported OCR engine: " + engine);
        }

        // 처리 결과 로깅
        long processingTime = System.currentTimeMillis() - startTime;
        log.info("⏱️  [OCR] Processing completed in {}ms", processingTime);
        log.info("📊 [OCR] Confidence: {}%", response.getConfidence());
        log.info("📝 [OCR] Extracted text length: {} characters", response.getExtractedText().length());

        // 추출된 텍스트 미리보기 (첫 200자만)
        String textPreview = response.getExtractedText().length() > 200
                ? response.getExtractedText().substring(0, 200) + "..."
                : response.getExtractedText();
        log.info("📄 [OCR] Extracted text preview:\n{}", textPreview);

        return response;
    }

    /**
     * Google Cloud Vision API를 사용하여 이미지에서 텍스트 추출
     * Reference: https://cloud.google.com/vision/docs/libraries
     */
    private OcrResponseDto extractTextWithGoogle(MultipartFile imageFile) throws IOException {
        log.info("🔍 [OCR] Starting text extraction with Google Vision");

        // MultipartFile → byte[]
        byte[] imageBytes = imageFile.getBytes();
        ByteString byteString = ByteString.copyFrom(imageBytes);

        // Google Credentials 설정 및 Vision API 클라이언트 생성 (try-with-resources)
        try (ImageAnnotatorClient vision = createVisionClient()) {
            // 이미지 생성
            Image image = Image.newBuilder()
                    .setContent(byteString)
                    .build();

            // TEXT_DETECTION 기능 설정 (한글/영어 모두 지원)
            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)
                    .build();

            // 한글 인식을 위한 언어 힌트 설정
            ImageContext imageContext = ImageContext.newBuilder()
                    .addLanguageHints("ko")
                    .addLanguageHints("en")
                    .build();

            // 요청 생성 (Google 문서 권장 패턴)
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(image)
                    .setImageContext(imageContext)
                    .build();

            // API 호출 (단일 요청인 경우 Collections.singletonList 사용)
            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(
                    Collections.singletonList(request)
            );

            List<AnnotateImageResponse> responses = response.getResponsesList();

            if (responses.isEmpty()) {
                log.warn("⚠️ [OCR] No responses from Vision API");
                return new OcrResponseDto("", 0);
            }

            // 첫 번째 응답 처리
            AnnotateImageResponse imageResponse = responses.get(0);

            // 에러 체크
            if (imageResponse.hasError()) {
                String errorMessage = imageResponse.getError().getMessage();
                log.error("❌ [OCR] Vision API error: {}", errorMessage);
                throw new RuntimeException("Vision API error: " + errorMessage);
            }

            // 텍스트 추출 (첫 번째 annotation이 전체 텍스트)
            List<EntityAnnotation> annotations = imageResponse.getTextAnnotationsList();
            if (annotations.isEmpty()) {
                log.warn("⚠️ [OCR] No text detected in image");
                return new OcrResponseDto("", 0);
            }

            // 첫 번째 annotation은 전체 텍스트를 포함
            EntityAnnotation fullTextAnnotation = annotations.get(0);
            String extractedText = fullTextAnnotation.getDescription();

            // Confidence 계산: 개별 단어들(2번째 이후 annotation)의 평균 신뢰도
            int confidence = calculateAverageConfidence(annotations);

            log.info("✅ [OCR] Google Vision extraction successful (confidence: {}%)", confidence);

            return new OcrResponseDto(extractedText, confidence);
        }
    }

    /**
     * Google Vision API 클라이언트 생성 (Credentials 명시적 설정)
     */
    private ImageAnnotatorClient createVisionClient() throws IOException {
        // Credentials 경로가 설정되어 있으면 명시적으로 로드
        if (StringUtils.hasText(googleCredentialsPath)) {
            log.info("🔑 [OCR] Loading Google credentials from: {}", googleCredentialsPath);

            try (FileInputStream credentialsStream = new FileInputStream(googleCredentialsPath)) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);

                ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                        .setCredentialsProvider(() -> credentials)
                        .build();

                return ImageAnnotatorClient.create(settings);
            }
        } else {
            // Credentials 경로가 없으면 기본 ADC 사용
            log.info("🔑 [OCR] Using Application Default Credentials");
            return ImageAnnotatorClient.create();
        }
    }

    /**
     * TEXT_DETECTION 결과에서 평균 신뢰도 계산
     * 첫 번째 annotation은 전체 텍스트이므로 제외하고, 개별 단어들의 평균 계산
     */
    private int calculateAverageConfidence(List<EntityAnnotation> annotations) {
        if (annotations.size() <= 1) {
            return 0; // 개별 단어 정보가 없는 경우
        }

        double totalConfidence = 0;
        int count = 0;

        // 첫 번째는 전체 텍스트이므로 2번째부터 개별 단어/구절
        for (int i = 1; i < annotations.size(); i++) {
            EntityAnnotation annotation = annotations.get(i);
            if (annotation.getConfidence() > 0) {
                totalConfidence += annotation.getConfidence();
                count++;
            }
        }

        return count > 0 ? (int) ((totalConfidence / count) * 100) : 0;
    }

    /**
     * Naver Clova OCR API를 사용하여 이미지에서 텍스트 추출 (Multipart/form-data)
     */
    private OcrResponseDto extractTextWithNaver(MultipartFile imageFile) {
        log.info("🔍 [OCR] Starting text extraction with Naver Clova");

        try {
            // 이미지 포맷 감지
            byte[] imageBytes = imageFile.getBytes();
            String imageFormat = detectImageFormat(imageBytes);
            log.info("🖼️ [OCR] Detected image format: {}", imageFormat);

            // Message JSON 생성 (Multipart의 message 파라미터)
            Map<String, Object> messageJson = new HashMap<>();
            messageJson.put("version", "V2");
            messageJson.put("requestId", String.valueOf(System.currentTimeMillis()));
            messageJson.put("timestamp", System.currentTimeMillis());
            messageJson.put("lang", "ko");

            List<Map<String, String>> images = new ArrayList<>();
            Map<String, String> imageInfo = new HashMap<>();
            imageInfo.put("format", imageFormat);
            imageInfo.put("name", imageFile.getOriginalFilename() != null ? imageFile.getOriginalFilename() : "snippet_image");
            images.add(imageInfo);

            messageJson.put("images", images);
            messageJson.put("enableTableDetection", false);

            String messageJsonString = objectMapper.writeValueAsString(messageJson);

            // Multipart 요청 바디 생성
            org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return imageFile.getOriginalFilename();
                }
            });
            body.add("message", messageJsonString);

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-OCR-SECRET", naverClovaSecretKey);

            HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            // API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    naverClovaApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("❌ [OCR] Naver Clova API error: {}", response.getStatusCode());
                throw new RuntimeException("Naver Clova OCR API failed: " + response.getStatusCode());
            }

            // 응답 파싱
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode imagesNode = jsonResponse.get("images");

            if (imagesNode == null || imagesNode.isEmpty()) {
                log.warn("⚠️ [OCR] No images in Naver response");
                return new OcrResponseDto("", 0);
            }

            JsonNode fieldsNode = imagesNode.get(0).get("fields");
            if (fieldsNode == null || fieldsNode.isEmpty()) {
                log.warn("⚠️ [OCR] No text detected in image");
                return new OcrResponseDto("", 0);
            }

            // 텍스트 추출
            StringBuilder extractedText = new StringBuilder();
            int totalConfidence = 0;
            int fieldCount = 0;

            for (JsonNode field : fieldsNode) {
                String inferText = field.get("inferText").asText();
                if (inferText != null && !inferText.isEmpty()) {
                    extractedText.append(inferText).append("\n");
                    totalConfidence += (int) (field.get("inferConfidence").asDouble() * 100);
                    fieldCount++;
                }
            }

            int avgConfidence = fieldCount > 0 ? totalConfidence / fieldCount : 0;
            String result = extractedText.toString().trim();

            log.info("✅ [OCR] Naver Clova extraction successful");

            return new OcrResponseDto(result, avgConfidence);

        } catch (Exception e) {
            log.error("❌ [OCR] Naver Clova error: {}", e.getMessage());
            throw new RuntimeException("Naver Clova OCR failed", e);
        }
    }

    /**
     * 이미지 바이트에서 포맷을 감지 (매직 넘버 기반)
     */
    private String detectImageFormat(byte[] imageBytes) {
        if (imageBytes.length < 4) {
            return "jpg"; // 기본값
        }

        // JPEG: FF D8 FF
        if (imageBytes[0] == (byte) 0xFF && imageBytes[1] == (byte) 0xD8 && imageBytes[2] == (byte) 0xFF) {
            return "jpg";
        }

        // PNG: 89 50 4E 47
        if (imageBytes[0] == (byte) 0x89 && imageBytes[1] == (byte) 0x50 &&
                imageBytes[2] == (byte) 0x4E && imageBytes[3] == (byte) 0x47) {
            return "png";
        }

        // GIF: 47 49 46
        if (imageBytes[0] == (byte) 0x47 && imageBytes[1] == (byte) 0x49 && imageBytes[2] == (byte) 0x46) {
            return "gif";
        }

        // WebP: 52 49 46 46 (RIFF) + WebP signature
        if (imageBytes[0] == (byte) 0x52 && imageBytes[1] == (byte) 0x49 &&
                imageBytes[2] == (byte) 0x46 && imageBytes[3] == (byte) 0x46) {
            return "webp";
        }

        // BMP: 42 4D
        if (imageBytes[0] == (byte) 0x42 && imageBytes[1] == (byte) 0x4D) {
            return "bmp";
        }

        // 감지 실패 시 기본값
        log.warn("⚠️ [OCR] Unknown image format, defaulting to jpg");
        return "jpg";
    }
}
