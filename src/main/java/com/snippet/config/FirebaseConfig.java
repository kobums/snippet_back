package com.snippet.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    /**
     * FCM은 앱이 등록된 Firebase 프로젝트의 서비스 계정 키가 필요하다.
     * Vision OCR용 GOOGLE_APPLICATION_CREDENTIALS와 프로젝트가 다를 수 있으므로
     * FIREBASE_CREDENTIALS를 우선 사용하고, 미설정 시 GOOGLE_APPLICATION_CREDENTIALS로 폴백한다.
     */
    @Value("${FIREBASE_CREDENTIALS:}")
    private String firebaseCredentialsPath;

    @Value("${GOOGLE_APPLICATION_CREDENTIALS:}")
    private String googleCredentialsPath;

    @PostConstruct
    public void initialize() {
        String credentialsPath = (firebaseCredentialsPath != null && !firebaseCredentialsPath.isBlank())
                ? firebaseCredentialsPath
                : googleCredentialsPath;
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("FIREBASE_CREDENTIALS / GOOGLE_APPLICATION_CREDENTIALS not set — FCM disabled");
            return;
        }
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase initialized");
        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage());
        }
    }
}
