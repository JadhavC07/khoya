package com.project.khoya.controller;

import com.google.firebase.FirebaseApp;
import com.project.khoya.dto.PushNotificationRequest;
import com.project.khoya.entity.FcmToken;
import com.project.khoya.repository.FcmTokenRepository;
import com.project.khoya.service.FirebaseMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugController {

    private final FcmTokenRepository fcmTokenRepository;
    private final FirebaseMessagingService firebaseMessagingService;

    @GetMapping("/tokens/{userId}")
    public ResponseEntity<?> getUserTokens(@PathVariable Long userId) {
        List<FcmToken> tokens = fcmTokenRepository.findByUserIdAndIsActiveTrue(userId);
        log.info("Found {} active tokens for user {}", tokens.size(), userId);

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "activeTokenCount", tokens.size(),
                "tokens", tokens.stream().map(token -> Map.of(
                        "id", token.getId(),
                        "token", token.getToken().substring(0, Math.min(50, token.getToken().length())) + "...",
                        "deviceType", token.getDeviceType(),
                        "deviceId", token.getDeviceId(),
                        "isActive", token.getIsActive(),
                        "createdAt", token.getCreatedAt()
                )).collect(Collectors.toList())
        ));
    }

    @GetMapping("/tokens/all")
    public ResponseEntity<?> getAllActiveTokens() {
        List<FcmToken> allTokens = fcmTokenRepository.findAllActiveTokens();
        log.info("Found {} total active tokens", allTokens.size());

        return ResponseEntity.ok(Map.of(
                "totalActiveTokens", allTokens.size(),
                "tokensByUser", allTokens.stream()
                        .collect(Collectors.groupingBy(
                                token -> token.getUser().getId(),
                                Collectors.counting()
                        ))
        ));
    }

    @PostMapping("/test-notification/{userId}")
    public ResponseEntity<?> testNotificationToUser(@PathVariable Long userId) {
        try {
            log.info("Sending test notification to user {}", userId);

            Map<String, String> data = Map.of(
                    "type", "TEST",
                    "timestamp", String.valueOf(System.currentTimeMillis())
            );

            firebaseMessagingService.sendPushNotificationToUser(
                    userId,
                    "🧪 Test Notification",
                    "This is a test notification to verify FCM integration",
                    data
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Test notification sent to user " + userId,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("Test notification failed for user {}", userId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to send test notification",
                    "message", e.getMessage(),
                    "userId", userId
            ));
        }
    }

    @PostMapping("/test-notification/all")
    public ResponseEntity<?> testNotificationToAll() {
        try {
            log.info("Sending test notification to all users");

            Map<String, String> data = Map.of(
                    "type", "BROADCAST_TEST",
                    "timestamp", String.valueOf(System.currentTimeMillis())
            );

            firebaseMessagingService.sendPushNotificationToAllUsers(
                    "📢 Broadcast Test",
                    "This is a test broadcast notification",
                    data
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Test broadcast notification sent",
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("Broadcast test notification failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to send broadcast test notification",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/test-notification/token")
    public ResponseEntity<?> testNotificationToToken(@RequestParam String token) {
        try {
            log.info("Sending test notification to specific token: {}...", token.substring(0, 20));

            Map<String, String> data = Map.of(
                    "type", "DIRECT_TOKEN_TEST",
                    "timestamp", String.valueOf(System.currentTimeMillis())
            );

            firebaseMessagingService.sendPushNotification(
                    PushNotificationRequest.builder()
                            .token(token)
                            .title("🎯 Direct Token Test")
                            .body("This notification was sent directly to your token")
                            .data(data)
                            .build()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Test notification sent to token",
                    "tokenPreview", token.substring(0, 20) + "...",
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("Direct token test notification failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to send direct token test notification",
                    "message", e.getMessage(),
                    "tokenPreview", token.substring(0, 20) + "..."
            ));
        }
    }


    @GetMapping("/firebase-status")
    public ResponseEntity<Map<String, Object>> getFirebaseStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            List<FirebaseApp> apps = FirebaseApp.getApps();
            status.put("initialized", !apps.isEmpty());
            status.put("appCount", apps.size());

            if (!apps.isEmpty()) {
                FirebaseApp defaultApp = FirebaseApp.getInstance();
                status.put("defaultAppName", defaultApp.getName());
                status.put("projectId", defaultApp.getOptions().getProjectId());
                status.put("serviceAccountId", defaultApp.getOptions().getServiceAccountId());
            } else {
                status.put("error", "No Firebase apps initialized");
            }

            // Environment variables check
            status.put("hasEnvVar", System.getenv("FIREBASE_SERVICE_ACCOUNT_KEY") != null);

        } catch (Exception e) {
            status.put("error", e.getMessage());
            status.put("initialized", false);
            log.error("Error checking Firebase status", e);
        }

        return ResponseEntity.ok(status);
    }
}