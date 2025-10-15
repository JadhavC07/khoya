package com.project.khoya.messaging;

import com.project.khoya.dto.AlertMessage;
import com.project.khoya.dto.FeatureExtractionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.social-media}")
    private String socialMediaRoutingKey;

    @Value("${rabbitmq.routing-key.notification}")
    private String notificationRoutingKey;

    @Value("${rabbitmq.routing-key.feature-extraction}")
    private String featureExtractionRoutingKey;


    public void sendSocialMediaMessage(AlertMessage message) {
        try {
            log.info("Sending social media message to queue for alert ID: {}", message.getAlertId());
            rabbitTemplate.convertAndSend(exchange, socialMediaRoutingKey, message);
            log.info("Social media message sent successfully for alert ID: {}", message.getAlertId());
        } catch (Exception e) {
            log.error("Failed to send social media message for alert ID: {}", message.getAlertId(), e);
        }
    }

    public void sendNotificationMessage(AlertMessage message) {
        try {
            log.info("Sending notification message to queue for alert ID: {}", message.getAlertId());
            rabbitTemplate.convertAndSend(exchange, notificationRoutingKey, message);
            log.info("Notification message sent successfully for alert ID: {}", message.getAlertId());
        } catch (Exception e) {
            log.error("Failed to send notification message for alert ID: {}", message.getAlertId(), e);
        }
    }

    public void sendFeatureExtractionMessage(FeatureExtractionMessage message) {
        try {
            log.info("Sending feature extraction message to queue for alert ID: {}", message.getAlertId());
            rabbitTemplate.convertAndSend(exchange, featureExtractionRoutingKey, message);
            log.info("Feature extraction message sent successfully for alert ID: {}", message.getAlertId());
        } catch (Exception e) {
            log.error("Failed to send feature extraction message for alert ID: {}", message.getAlertId(), e);
        }
    }
}