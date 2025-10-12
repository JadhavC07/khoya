package com.project.khoya.controller;

import com.project.khoya.config.CustomUserDetails;
import com.project.khoya.dto.FcmTokenRequest;
import com.project.khoya.service.FcmTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Push notification management APIs")
public class NotificationController {

    private final FcmTokenService fcmTokenService;

    @PostMapping("/token")
    @Operation(summary = "Register FCM token", description = "Register or update FCM token for push notifications", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> registerToken(@Valid @RequestBody FcmTokenRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {

        fcmTokenService.saveOrUpdateToken(userDetails.getUser().getId(), request);

        return ResponseEntity.ok(Map.of("status", "success", "message", "FCM token registered successfully"));
    }

    @DeleteMapping("/token")
    @Operation(summary = "Remove FCM token", description = "Remove FCM token (on logout)", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> removeToken(@RequestParam String token, @AuthenticationPrincipal CustomUserDetails userDetails) {

        fcmTokenService.removeToken(userDetails.getUser().getId(), token);

        return ResponseEntity.ok(Map.of("status", "success", "message", "FCM token removed successfully"));
    }
}