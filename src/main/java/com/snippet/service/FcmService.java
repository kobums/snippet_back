package com.snippet.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    public void sendNotification(String fcmToken, String title, String body) {
        if (fcmToken == null || fcmToken.isBlank())
            return;
        if (FirebaseApp.getApps().isEmpty()) {
            log.debug("Firebase not initialized — skipping notification");
            return;
        }
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.error("FCM send failed for token {}: {}", fcmToken.substring(0, Math.min(10, fcmToken.length())),
                    e.getMessage());
        }
    }
}
