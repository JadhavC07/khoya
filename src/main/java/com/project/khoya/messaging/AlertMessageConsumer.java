package com.project.khoya.messaging;

import com.project.khoya.dto.AlertMessage;
import com.project.khoya.entity.MissingAlert;
import com.project.khoya.repository.MissingAlertRepository;
import com.project.khoya.service.FacebookService;
import com.project.khoya.service.FirebaseMessagingService;
import com.project.khoya.service.InstagramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertMessageConsumer {

    private final InstagramService instagramService;
    private final FacebookService facebookService;
    private final FirebaseMessagingService firebaseMessagingService;
    private final MissingAlertRepository alertRepository;

    @RabbitListener(queues = "${rabbitmq.queue.social-media}")
    public void consumeSocialMediaMessage(AlertMessage message) {
        log.info("Received social media message for alert ID: {}", message.getAlertId());

        try {
            // Fetch the full alert from database
            MissingAlert alert = alertRepository.findById(message.getAlertId())
                    .orElse(null);

            if (alert == null) {
                log.error("Alert not found with ID: {}", message.getAlertId());
                return;
            }

            if (alert.getImageUrl() == null || alert.getImageUrl().isEmpty()) {
                log.warn("No image URL for alert ID {}. Skipping social media posts.", alert.getId());
                return;
            }

            // Post to Instagram
            try {
                boolean instagramSuccess = instagramService.postAlertToInstagram(alert);
                if (instagramSuccess) {
                    log.info("Successfully posted alert ID {} to Instagram", alert.getId());
                } else {
                    log.warn("Failed to post alert ID {} to Instagram", alert.getId());
                }
            } catch (Exception e) {
                log.error("Exception posting alert ID {} to Instagram: {}", alert.getId(), e.getMessage(), e);
            }

            // Post to Facebook
            try {
                boolean facebookSuccess = facebookService.postAlertToFacebook(alert);
                if (facebookSuccess) {
                    log.info("Successfully posted alert ID {} to Facebook", alert.getId());
                } else {
                    log.warn("Failed to post alert ID {} to Facebook", alert.getId());
                }
            } catch (Exception e) {
                log.error("Exception posting alert ID {} to Facebook: {}", alert.getId(), e.getMessage(), e);
            }

            log.info("Completed social media posting for alert ID: {}", alert.getId());

        } catch (Exception e) {
            log.error("Error processing social media message for alert ID: {}", message.getAlertId(), e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.notification}")
    public void consumeNotificationMessage(AlertMessage message) {
        log.info("Received notification message for alert ID: {}", message.getAlertId());

        try {
            Map<String, String> data = Map.of(
                    "alertId", message.getAlertId().toString(),
                    "type", "NEW_ALERT",
                    "location", message.getLocation() != null ? message.getLocation() : ""
            );

            firebaseMessagingService.sendPushNotificationToAllUsers(
                    "🚨 New Missing Person Alert",
                    message.getTitle() + " reported missing in " + message.getLocation(),
                    data
            );

            log.info("Successfully sent notification for alert ID: {}", message.getAlertId());

        } catch (Exception e) {
            log.error("Failed to send notification for alert ID {}: {}", message.getAlertId(), e.getMessage(), e);
        }
    }
}