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
            sendPushNotification(PushNotificationRequest.builder().token(token.getToken()).title(title).body(body).data(data).build());
        }
    }

    public void sendPushNotificationToAllUsers(String title, String body, Map<String, String> data) {
        List<FcmToken> allTokens = fcmTokenRepository.findAllActiveTokens();
        int batchSize = 500;
        for (int i = 0; i < allTokens.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allTokens.size());
            List<FcmToken> batch = allTokens.subList(i, end);
            CompletableFuture.runAsync(() -> sendBatchNotifications(batch, title, body, data));
        }
    }

    private void sendBatchNotifications(List<FcmToken> tokens, String title, String body, Map<String, String> data) {
        List<String> tokenStrings = tokens.stream().map(FcmToken::getToken).toList();

        if (tokenStrings.isEmpty()) {

            return;
        }
        MulticastMessage message = MulticastMessage.builder().setNotification(Notification.builder().setTitle(title).setBody(body).build()).putAllData(flattenData(data)).addAllTokens(tokenStrings).setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build()).setApnsConfig(ApnsConfig.builder().setAps(Aps.builder().setContentAvailable(true).build()).build()).build();
        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            if (response.getFailureCount() > 0) {
                handleFailedTokens(response, tokens);
            }
        } catch (FirebaseMessagingException e) {

        }
    }

    public void sendPushNotification(PushNotificationRequest request) {
        Message message = Message.builder().setNotification(Notification.builder().setTitle(request.getTitle()).setBody(request.getBody()).build()).putAllData(flattenData(request.getData())).setToken(request.getToken()).setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build()).setApnsConfig(ApnsConfig.builder().setAps(Aps.builder().setContentAvailable(true).build()).build()).build();
        try {
            String response = FirebaseMessaging.getInstance().send(message);

        } catch (FirebaseMessagingException e) {

            if (isInvalidToken(e)) {
                deactivateInvalidToken(request.getToken());
            }
        }
    }

    private Map<String, String> flattenData(Map<String, String> data) {
        return data.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue() instanceof String ? e.getValue() : e.getValue()));
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
        fcmTokenRepository.findByTokenAndIsActiveTrue(token).ifPresent(t -> {
            t.setIsActive(false);
            fcmTokenRepository.save(t);

        });
    }

    private boolean isInvalidToken(FirebaseMessagingException e) {
        return e.getErrorCode() == ErrorCode.INVALID_ARGUMENT || e.getErrorCode() == ErrorCode.UNAVAILABLE || "registration-token-not-registered".equalsIgnoreCase(e.getMessagingErrorCode().name());
    }
}