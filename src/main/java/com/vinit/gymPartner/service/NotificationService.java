package com.vinit.gymPartner.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.vinit.gymPartner.entity.DeviceToken;
import com.vinit.gymPartner.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final DeviceTokenRepository deviceTokenRepository;

    public void sendToUser(Long userId, String title, String body) {
        // Get all device tokens for this user
        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(userId);

        if (tokens.isEmpty()) {
            log.info("No device tokens found for user {}, skipping notification", userId);
            return;
        }

        for (DeviceToken deviceToken : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(deviceToken.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .build();

                String response = FirebaseMessaging.getInstance().send(message);
                log.info("Notification sent successfully: {}", response);

            } catch (FirebaseMessagingException e) {
                log.warn("Failed to send notification to token: {}. Error: {}",
                        deviceToken.getToken(), e.getMessage());
                // Cleanup invalid/expired tokens automatically
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    deviceTokenRepository.delete(deviceToken);
                    log.info("Removed invalid/expired token: {}", deviceToken.getToken());
                }
            } catch (Exception e) {
                log.error("Unexpected error sending notification to user {}: {}",
                        userId, e.getMessage());
            }
        }
    }
}