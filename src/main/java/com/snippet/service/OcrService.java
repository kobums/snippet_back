package com.snippet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.snippet.dto.OcrRegionDto;
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
     * @param regions 밑줄 영역 목록 (이미지 픽셀 좌표). null 또는 빈 리스트면 전체 텍스트 반환.
     */
    public OcrResponseDto extractText(MultipartFile imageFile, String engine, List<OcrRegionDto> regions) throws IOException {
        if (engine == null || engine.isEmpty() || "google".equalsIgnoreCase(engine)) {
            return extractTextWithGoogle(imageFile, regions);
        } else if ("naver".equalsIgnoreCase(engine)) {
            return extractTextWithNaver(imageFile, regions);
        } else {
            throw new IllegalArgumentException("Unsupported OCR engine: " + engine);
        }
    }

    private OcrResponseDto extractTextWithGoogle(MultipartFile imageFile, List<OcrRegionDto> regions) throws IOException {
        byte[] imageBytes = imageFile.getBytes();
        ByteString byteString = ByteString.copyFrom(imageBytes);

        try (ImageAnnotatorClient vision = createVisionClient()) {
            Image image = Image.newBuilder().setContent(byteString).build();
            Feature feature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
            ImageContext imageContext = ImageContext.newBuilder()
                    .addLanguageHints("ko")
                    .addLanguageHints("en")
                    .build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(image)
                    .setImageContext(imageContext)
                    .build();

            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(
                    Collections.singletonList(request)
            );
            List<AnnotateImageResponse> responses = response.getResponsesList();

            if (responses.isEmpty()) return new OcrResponseDto("", 0);

            AnnotateImageResponse imageResponse = responses.get(0);
            if (imageResponse.hasError()) {
                throw new RuntimeException("Vision API error: " + imageResponse.getError().getMessage());
            }

            List<EntityAnnotation> annotations = imageResponse.getTextAnnotationsList();
            if (annotations.isEmpty()) return new OcrResponseDto("", 0);

            // 영역 필터링 없으면 전체 텍스트 반환
            if (regions == null || regions.isEmpty()) {
                String extractedText = annotations.get(0).getDescription();
                int confidence = calculateAverageConfidence(annotations);
                return new OcrResponseDto(extractedText, confidence);
            }

            // 단어 단위 어노테이션(index 1+)을 영역 필터링 후 조합
            StringBuilder filtered = new StringBuilder();
            int totalConf = 0, count = 0;

            for (int i = 1; i < annotations.size(); i++) {
                EntityAnnotation word = annotations.get(i);
                BoundingPoly poly = word.getBoundingPoly();
                if (poly.getVerticesCount() == 0) continue;

                double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
                double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
                for (Vertex v : poly.getVerticesList()) {
                    minX = Math.min(minX, v.getX());
                    minY = Math.min(minY, v.getY());
                    maxX = Math.max(maxX, v.getX());
                    maxY = Math.max(maxY, v.getY());
                }

                if (overlapsAny(minX, minY, maxX, maxY, regions)) {
                    filtered.append(word.getDescription()).append(" ");
                    if (word.getConfidence() > 0) {
                        totalConf += (int) (word.getConfidence() * 100);
                        count++;
                    }
                }
            }

            int avgConf = count > 0 ? totalConf / count : 0;
            return new OcrResponseDto(filtered.toString().trim(), avgConf);
        }
    }

    private ImageAnnotatorClient createVisionClient() throws IOException {
        if (StringUtils.hasText(googleCredentialsPath)) {
            try (FileInputStream credentialsStream = new FileInputStream(googleCredentialsPath)) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
                ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                        .setCredentialsProvider(() -> credentials)
                        .build();
                return ImageAnnotatorClient.create(settings);
            }
        } else {
            return ImageAnnotatorClient.create();
        }
    }

    private int calculateAverageConfidence(List<EntityAnnotation> annotations) {
        if (annotations.size() <= 1) return 0;
        double totalConfidence = 0;
        int count = 0;
        for (int i = 1; i < annotations.size(); i++) {
            EntityAnnotation annotation = annotations.get(i);
            if (annotation.getConfidence() > 0) {
                totalConfidence += annotation.getConfidence();
                count++;
            }
        }
        return count > 0 ? (int) ((totalConfidence / count) * 100) : 0;
    }

    private OcrResponseDto extractTextWithNaver(MultipartFile imageFile, List<OcrRegionDto> regions) {
        try {
            byte[] imageBytes = imageFile.getBytes();
            String imageFormat = detectImageFormat(imageBytes);

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

            org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return imageFile.getOriginalFilename();
                }
            });
            body.add("message", messageJsonString);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-OCR-SECRET", naverClovaSecretKey);

            HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(naverClovaApiUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Naver Clova OCR API failed: " + response.getStatusCode());
            }

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode imagesNode = jsonResponse.get("images");
            if (imagesNode == null || imagesNode.isEmpty()) return new OcrResponseDto("", 0);

            JsonNode fieldsNode = imagesNode.get(0).get("fields");
            if (fieldsNode == null || fieldsNode.isEmpty()) return new OcrResponseDto("", 0);

            StringBuilder extractedText = new StringBuilder();
            int totalConfidence = 0, fieldCount = 0;

            for (JsonNode field : fieldsNode) {
                String inferText = field.get("inferText").asText();
                if (inferText == null || inferText.isEmpty()) continue;

                // 영역 필터링
                if (regions != null && !regions.isEmpty() && !overlapsAnyField(field, regions)) {
                    continue;
                }

                extractedText.append(inferText).append(" ");
                totalConfidence += (int) (field.get("inferConfidence").asDouble() * 100);
                fieldCount++;
            }

            int avgConfidence = fieldCount > 0 ? totalConfidence / fieldCount : 0;
            return new OcrResponseDto(extractedText.toString().trim(), avgConfidence);

        } catch (Exception e) {
            throw new RuntimeException("Naver Clova OCR failed", e);
        }
    }

    /** 단어의 bounding rect가 하나 이상의 region과 겹치는지 확인 */
    private boolean overlapsAny(double wLeft, double wTop, double wRight, double wBottom, List<OcrRegionDto> regions) {
        for (OcrRegionDto region : regions) {
            if (wLeft < region.getRight() && wRight > region.getLeft()
                    && wTop < region.getBottom() && wBottom > region.getTop()) {
                return true;
            }
        }
        return false;
    }

    /** Naver Clova field의 boundingPoly vertices로 영역 겹침 확인 */
    private boolean overlapsAnyField(JsonNode field, List<OcrRegionDto> regions) {
        JsonNode poly = field.get("boundingPoly");
        if (poly == null) return false;
        JsonNode vertices = poly.get("vertices");
        if (vertices == null || !vertices.isArray() || vertices.size() == 0) return false;

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (JsonNode v : vertices) {
            double x = v.get("x").asDouble();
            double y = v.get("y").asDouble();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        return overlapsAny(minX, minY, maxX, maxY, regions);
    }

    private String detectImageFormat(byte[] imageBytes) {
        if (imageBytes.length < 4) return "jpg";
        if (imageBytes[0] == (byte) 0xFF && imageBytes[1] == (byte) 0xD8 && imageBytes[2] == (byte) 0xFF) return "jpg";
        if (imageBytes[0] == (byte) 0x89 && imageBytes[1] == (byte) 0x50
                && imageBytes[2] == (byte) 0x4E && imageBytes[3] == (byte) 0x47) return "png";
        if (imageBytes[0] == (byte) 0x47 && imageBytes[1] == (byte) 0x49 && imageBytes[2] == (byte) 0x46) return "gif";
        if (imageBytes[0] == (byte) 0x52 && imageBytes[1] == (byte) 0x49
                && imageBytes[2] == (byte) 0x46 && imageBytes[3] == (byte) 0x46) return "webp";
        if (imageBytes[0] == (byte) 0x42 && imageBytes[1] == (byte) 0x4D) return "bmp";
        return "jpg";
    }
}
