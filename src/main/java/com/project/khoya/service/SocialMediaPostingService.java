package com.project.khoya.service;

import com.project.khoya.entity.MissingAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialMediaPostingService {

    private final InstagramService instagramService;
    private final FacebookService facebookService;
    private final FirebaseMessagingService firebaseMessagingService;

    @Async("socialMediaExecutor")
    public void postToSocialMediaAsync(MissingAlert alert) {
        if (alert.getImageUrl() == null) {
            return;
        }

        // Post to Instagram
        try {
            boolean instagramSuccess = instagramService.postAlertToInstagram(alert);

        } catch (Exception e) {
        }

        // Post to Facebook
        try {
            boolean facebookSuccess = facebookService.postAlertToFacebook(alert);

        } catch (Exception e) {
        }

    }


    @Async("socialMediaExecutor")
    public void sendNotificationAsync(MissingAlert alert) {

        try {
            Map<String, String> data = Map.of(
                    "alertId", alert.getId().toString(),
                    "type", "NEW_ALERT",
                    "location", alert.getLocation() != null ? alert.getLocation() : ""
            );

            firebaseMessagingService.sendPushNotificationToAllUsers(
                    "🚨 New Missing Person Alert",
                    alert.getTitle() + " reported missing in " + alert.getLocation(),
                    data
            );

        } catch (Exception e) {
            log.error("Failed to send async notification for alert ID {}: {}", alert.getId(), e.getMessage(), e);
        }
    }
}