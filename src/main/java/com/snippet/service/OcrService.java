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

    public OcrResponseDto extractText(MultipartFile imageFile, String engine) throws IOException {
        OcrResponseDto response;
        if (engine == null || engine.isEmpty() || "google".equalsIgnoreCase(engine)) {
            response = extractTextWithGoogle(imageFile);
        } else if ("naver".equalsIgnoreCase(engine)) {
            response = extractTextWithNaver(imageFile);
        } else {
            throw new IllegalArgumentException("Unsupported OCR engine: " + engine);
        }

        return response;
    }

    private OcrResponseDto extractTextWithGoogle(MultipartFile imageFile) throws IOException {
        byte[] imageBytes = imageFile.getBytes();
        ByteString byteString = ByteString.copyFrom(imageBytes);

        try (ImageAnnotatorClient vision = createVisionClient()) {
            Image image = Image.newBuilder()
                    .setContent(byteString)
                    .build();

            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)
                    .build();

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

            if (responses.isEmpty()) {
                return new OcrResponseDto("", 0);
            }

            AnnotateImageResponse imageResponse = responses.get(0);

            if (imageResponse.hasError()) {
                String errorMessage = imageResponse.getError().getMessage();
                throw new RuntimeException("Vision API error: " + errorMessage);
            }

            List<EntityAnnotation> annotations = imageResponse.getTextAnnotationsList();
            if (annotations.isEmpty()) {
                return new OcrResponseDto("", 0);
            }

            EntityAnnotation fullTextAnnotation = annotations.get(0);
            String extractedText = fullTextAnnotation.getDescription();

            int confidence = calculateAverageConfidence(annotations);

            return new OcrResponseDto(extractedText, confidence);
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
        if (annotations.size() <= 1) {
            return 0;
        }

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

    private OcrResponseDto extractTextWithNaver(MultipartFile imageFile) {
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

            ResponseEntity<String> response = restTemplate.exchange(
                    naverClovaApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Naver Clova OCR API failed: " + response.getStatusCode());
            }

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode imagesNode = jsonResponse.get("images");

            if (imagesNode == null || imagesNode.isEmpty()) {
                return new OcrResponseDto("", 0);
            }

            JsonNode fieldsNode = imagesNode.get(0).get("fields");
            if (fieldsNode == null || fieldsNode.isEmpty()) {
                return new OcrResponseDto("", 0);
            }

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

            return new OcrResponseDto(result, avgConfidence);

        } catch (Exception e) {
            throw new RuntimeException("Naver Clova OCR failed", e);
        }
    }

    private String detectImageFormat(byte[] imageBytes) {
        if (imageBytes.length < 4) {
            return "jpg";
        }

        if (imageBytes[0] == (byte) 0xFF && imageBytes[1] == (byte) 0xD8 && imageBytes[2] == (byte) 0xFF) {
            return "jpg";
        }

        if (imageBytes[0] == (byte) 0x89 && imageBytes[1] == (byte) 0x50 &&
                imageBytes[2] == (byte) 0x4E && imageBytes[3] == (byte) 0x47) {
            return "png";
        }

        if (imageBytes[0] == (byte) 0x47 && imageBytes[1] == (byte) 0x49 && imageBytes[2] == (byte) 0x46) {
            return "gif";
        }

        if (imageBytes[0] == (byte) 0x52 && imageBytes[1] == (byte) 0x49 &&
                imageBytes[2] == (byte) 0x46 && imageBytes[3] == (byte) 0x46) {
            return "webp";
        }

        if (imageBytes[0] == (byte) 0x42 && imageBytes[1] == (byte) 0x4D) {
            return "bmp";
        }

        return "jpg";
    }
}
