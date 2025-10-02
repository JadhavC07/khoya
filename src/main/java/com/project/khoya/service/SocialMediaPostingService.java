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
            log.warn("No image URL for alert ID {}. Skipping social media posts.", alert.getId());
            return;
        }

        log.info("Starting async social media posting for alert ID: {}", alert.getId());

        // Post to Instagram
        try {
            boolean instagramSuccess = instagramService.postAlertToInstagram(alert);
            if (instagramSuccess) {
                log.info("Successfully posted alert ID {} to Instagram (async)", alert.getId());
            } else {
                log.warn("Failed to post alert ID {} to Instagram (async)", alert.getId());
            }
        } catch (Exception e) {
            log.error("Exception posting alert ID {} to Instagram (async): {}", alert.getId(), e.getMessage(), e);
        }

        // Post to Facebook
        try {
            boolean facebookSuccess = facebookService.postAlertToFacebook(alert);
            if (facebookSuccess) {
                log.info("Successfully posted alert ID {} to Facebook (async)", alert.getId());
            } else {
                log.warn("Failed to post alert ID {} to Facebook (async)", alert.getId());
            }
        } catch (Exception e) {
            log.error("Exception posting alert ID {} to Facebook (async): {}", alert.getId(), e.getMessage(), e);
        }

        log.info("Completed async social media posting for alert ID: {}", alert.getId());
    }


    @Async("socialMediaExecutor")
    public void sendNotificationAsync(MissingAlert alert) {
        log.info("Sending async notification for alert ID: {}", alert.getId());

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

            log.info("Successfully sent async notification for alert ID: {}", alert.getId());
        } catch (Exception e) {
            log.error("Failed to send async notification for alert ID {}: {}", alert.getId(), e.getMessage(), e);
        }
    }
}