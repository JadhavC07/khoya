package com.project.khoya.service;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.*;
import com.project.khoya.dto.PushNotificationRequest;
import com.project.khoya.entity.FcmToken;
import com.project.khoya.entity.User;
import com.project.khoya.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseMessagingService {

    private final FcmTokenRepository fcmTokenRepository;

    public void sendPushNotificationToUser(Long userId, String title, String body, Map<String, String> data) {
        List<FcmToken> tokens = fcmTokenRepository.findByUserIdAndIsActiveTrue(userId);

        for (FcmToken fcmToken : tokens) {
            sendPushNotification(PushNotificationRequest.builder()
                    .token(fcmToken.getToken())
                    .title(title)
                    .body(body)
                    .data(data)
                    .build());
        }
    }

    public void sendPushNotificationToAllUsers(String title, String body, Map<String, String> data) {
        List<FcmToken> allTokens = fcmTokenRepository.findAllActiveTokens();

        // Send in batches to avoid rate limits
        int batchSize = 100;
        for (int i = 0; i < allTokens.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allTokens.size());
            List<FcmToken> batch = allTokens.subList(i, end);

            CompletableFuture.runAsync(() -> sendBatchNotifications(batch, title, body, data));
        }
    }

    private void sendBatchNotifications(List<FcmToken> tokens, String title, String body, Map<String, String> data) {
        List<String> tokenStrings = tokens.stream()
                .map(FcmToken::getToken)
                .toList();

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .addAllTokens(tokenStrings)
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);
            log.info("Successfully sent {} messages out of {}", response.getSuccessCount(), tokens.size());

            // Handle invalid tokens
            if (response.getFailureCount() > 0) {
                handleFailedTokens(response, tokens);
            }
        } catch (FirebaseMessagingException e) {
            log.error("Error sending batch notifications", e);
        }
    }

    public void sendPushNotification(PushNotificationRequest request) {
        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(request.getTitle())
                        .setBody(request.getBody())
                        .build())
                .putAllData(request.getData())
                .setToken(request.getToken())
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent message: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Error sending push notification to token: {}", request.getToken(), e);

            // Remove invalid tokens
            if (isInvalidToken(e)) {
                fcmTokenRepository.findByTokenAndIsActiveTrue(request.getToken())
                        .ifPresent(token -> {
                            token.setIsActive(false);
                            fcmTokenRepository.save(token);
                        });
            }
        }
    }

    private void handleFailedTokens(BatchResponse response, List<FcmToken> tokens) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {
                FcmToken token = tokens.get(i);
                Exception exception = responses.get(i).getException();

                if (exception instanceof FirebaseMessagingException) {
                    FirebaseMessagingException fme = (FirebaseMessagingException) exception;
                    if (isInvalidToken(fme)) {
                        token.setIsActive(false);
                        fcmTokenRepository.save(token);
                        log.info("Deactivated invalid token: {}", token.getToken());
                    }
                }
            }
        }
    }

    private boolean isInvalidToken(FirebaseMessagingException e) {
        return e.getErrorCode() == ErrorCode.INVALID_ARGUMENT ||
                e.getErrorCode() == ErrorCode.UNAUTHENTICATED;
    }
}