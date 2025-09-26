//package com.project.khoya.service;
//
//
//import com.google.firebase.ErrorCode;
//import com.google.firebase.messaging.*;
//import com.project.khoya.dto.PushNotificationRequest;
//import com.project.khoya.entity.FcmToken;
//import com.project.khoya.entity.User;
//import com.project.khoya.repository.FcmTokenRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class FirebaseMessagingService {
//
//    private final FcmTokenRepository fcmTokenRepository;
//
//    /**
//     * Send notification to a single user.
//     */
//    public void sendPushNotificationToUser(Long userId, String title, String body, Map<String, String> data) {
//        List<FcmToken> tokens = fcmTokenRepository.findByUserIdAndIsActiveTrue(userId);
//
//        for (FcmToken fcmToken : tokens) {
//            sendPushNotification(PushNotificationRequest.builder()
//                    .token(fcmToken.getToken())
//                    .title(title)
//                    .body(body)
//                    .data(data)
//                    .build());
//        }
//    }
//
//    /**
//     * Broadcast to all active users.
//     */
//    public void sendPushNotificationToAllUsers(String title, String body, Map<String, String> data) {
//        List<FcmToken> allTokens = fcmTokenRepository.findAllActiveTokens();
//
//        int batchSize = 50; // Firebase allows up to 500 per batch
//        for (int i = 0; i < allTokens.size(); i += batchSize) {
//            int end = Math.min(i + batchSize, allTokens.size());
//            List<FcmToken> batch = allTokens.subList(i, end);
//
//            CompletableFuture.runAsync(() -> sendBatchNotifications(batch, title, body, data));
//        }
//    }
//
//    /**
//     * Batch send (up to 500 tokens).
//     */
//    private void sendBatchNotifications(List<FcmToken> tokens, String title, String body, Map<String, String> data) {
//        List<String> tokenStrings = tokens.stream()
//                .map(FcmToken::getToken)
//                .toList();
//
//        MulticastMessage message = MulticastMessage.builder()
//                .setNotification(Notification.builder()
//                        .setTitle(title)
//                        .setBody(body)
//                        .build())
//                .putAllData(data)
//                .addAllTokens(tokenStrings)
//                .build();
//
//        try {
//            BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);
//            log.info("✅ Successfully sent {} messages out of {}", response.getSuccessCount(), tokens.size());
//
//            if (response.getFailureCount() > 0) {
//                handleFailedTokens(response, tokens);
//            }
//        } catch (FirebaseMessagingException e) {
//            log.error("🔥 Error sending batch notifications", e);
//        }
//    }
//
//    /**
//     * Single push send.
//     */
//    public void sendPushNotification(PushNotificationRequest request) {
//        Message message = Message.builder()
//                .setNotification(Notification.builder()
//                        .setTitle(request.getTitle())
//                        .setBody(request.getBody())
//                        .build())
//                .putAllData(request.getData())
//                .setToken(request.getToken())
//                .build();
//
//        try {
//            String response = FirebaseMessaging.getInstance().send(message);
//            log.info("✅ Successfully sent message: {}", response);
//        } catch (FirebaseMessagingException e) {
//            log.error("🔥 Error sending push notification to token: {}", request.getToken(), e);
//
//            if (isInvalidToken(e)) {
//                deactivateInvalidToken(request.getToken());
//            }
//        }
//    }
//
//    /**
//     * Handle failed tokens in a batch.
//     */
//    private void handleFailedTokens(BatchResponse response, List<FcmToken> tokens) {
//        List<SendResponse> responses = response.getResponses();
//        for (int i = 0; i < responses.size(); i++) {
//            if (!responses.get(i).isSuccessful()) {
//                Exception exception = responses.get(i).getException();
//                if (exception instanceof FirebaseMessagingException fme && isInvalidToken(fme)) {
//                    deactivateInvalidToken(tokens.get(i).getToken());
//                }
//            }
//        }
//    }
//
//    /**
//     * Deactivate a token in DB.
//     */
//    private void deactivateInvalidToken(String token) {
//        fcmTokenRepository.findByTokenAndIsActiveTrue(token)
//                .ifPresent(t -> {
//                    t.setIsActive(false);
//                    fcmTokenRepository.save(t);
//                    log.info("🚫 Deactivated invalid token: {}", token);
//                });
//    }
//
//    /**
//     * Determine if an error indicates invalid/expired token.
//     */
//    private boolean isInvalidToken(FirebaseMessagingException e) {
//        return e.getErrorCode() == ErrorCode.INVALID_ARGUMENT ||
//                e.getErrorCode() == ErrorCode.UNAUTHENTICATED ||
//                "registration-token-not-registered".equalsIgnoreCase(e.getMessagingErrorCode().name());
//    }
//}


package com.project.khoya.service;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.*;
import com.project.khoya.dto.PushNotificationRequest;
import com.project.khoya.entity.FcmToken;
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
        for (FcmToken token : tokens) {
            sendPushNotification(PushNotificationRequest.builder()
                    .token(token.getToken())
                    .title(title)
                    .body(body)
                    .data(data)
                    .build());
        }
    }

    public void sendPushNotificationToAllUsers(String title, String body, Map<String, String> data) {
        List<FcmToken> allTokens = fcmTokenRepository.findAllActiveTokens();
        int batchSize = 500; // HTTP v1 limit
        for (int i = 0; i < allTokens.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allTokens.size());
            List<FcmToken> batch = allTokens.subList(i, end);
            CompletableFuture.runAsync(() -> sendBatchNotifications(batch, title, body, data));
        }
    }

    private void sendBatchNotifications(List<FcmToken> tokens, String title, String body, Map<String, String> data) {
        List<String> tokenStrings = tokens.stream().map(FcmToken::getToken).toList();
        log.info("🔍 Sending HTTP v1 batch to {} tokens", tokenStrings.size());
        if (tokenStrings.isEmpty()) {
            log.warn("⚠️ No valid tokens for batch notification");
            return;
        }
        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(flattenData(data)) // Flatten nested JSON for v1
                .addAllTokens(tokenStrings)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setContentAvailable(true)
                                .build())
                        .build())
                .build();
        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("✅ HTTP v1 batch: {} successes, {} failures out of {}",
                    response.getSuccessCount(), response.getFailureCount(), tokenStrings.size());
            if (response.getFailureCount() > 0) {
                handleFailedTokens(response, tokens);
            }
        } catch (FirebaseMessagingException e) {
            log.error("🔥 HTTP v1 batch failed: {} (Code: {}, MessagingCode: {})",
                    e.getMessage(), e.getErrorCode(), e.getMessagingErrorCode(), e);
        }
    }

    public void sendPushNotification(PushNotificationRequest request) {
        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(request.getTitle())
                        .setBody(request.getBody())
                        .build())
                .putAllData(flattenData(request.getData()))
                .setToken(request.getToken())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setContentAvailable(true)
                                .build())
                        .build())
                .build();
        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("✅ HTTP v1 message sent: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("🔥 HTTP v1 message failed for token {}: {} (Code: {})",
                    request.getToken(), e.getMessage(), e.getErrorCode(), e);
            if (isInvalidToken(e)) {
                deactivateInvalidToken(request.getToken());
            }
        }
    }

    private Map<String, String> flattenData(Map<String, String> data) {
        return data.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() instanceof String ? (String) e.getValue() : e.getValue().toString()
                ));
    }

    private void handleFailedTokens(BatchResponse response, List<FcmToken> tokens) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {
                Exception exception = responses.get(i).getException();
                if (exception instanceof FirebaseMessagingException fme && isInvalidToken(fme)) {
                    deactivateInvalidToken(tokens.get(i).getToken());
                }
            }
        }
    }

    private void deactivateInvalidToken(String token) {
        fcmTokenRepository.findByTokenAndIsActiveTrue(token)
                .ifPresent(t -> {
                    t.setIsActive(false);
                    fcmTokenRepository.save(t);
                    log.info("🚫 Deactivated invalid token: {}", token);
                });
    }

    private boolean isInvalidToken(FirebaseMessagingException e) {
        return e.getErrorCode() == ErrorCode.INVALID_ARGUMENT ||
                e.getErrorCode() == ErrorCode.UNAVAILABLE ||
                "registration-token-not-registered".equalsIgnoreCase(e.getMessagingErrorCode().name());
    }
}