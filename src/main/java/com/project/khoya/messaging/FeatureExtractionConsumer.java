package com.project.khoya.messaging;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import com.project.khoya.dto.FeatureExtractionMessage;
import com.project.khoya.entity.MissingAlert;
import com.project.khoya.repository.MissingAlertRepository;
import com.project.khoya.service.Image.FeatureExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureExtractionConsumer {

    private final FeatureExtractionService featureExtractionService;
    private final MissingAlertRepository alertRepository;

    @RabbitListener(queues = "${rabbitmq.queue.feature-extraction}")
    public void consumeFeatureExtractionMessage(FeatureExtractionMessage message) {
        log.info("Received feature extraction message for alert ID: {}", message.getAlertId());

        try {
            MissingAlert alert = alertRepository.findById(message.getAlertId()).orElse(null);

            if (alert == null) {
                log.error("Alert not found with ID: {}", message.getAlertId());
                return;
            }

            if (message.getImageUrl() == null || message.getImageUrl().isEmpty()) {
                log.warn("No image URL for alert ID {}. Skipping feature extraction.", alert.getId());
                return;
            }

            // Download image from Cloudinary and extract features
            Image image = ImageFactory.getInstance().fromUrl(message.getImageUrl());
            byte[] embedding = featureExtractionService.extractFeaturesAsBytes(image);

            alert.setImageEmbedding(embedding);
            alertRepository.save(alert);

            log.info("Successfully extracted and saved features for alert ID: {} (size: {} bytes)", alert.getId(), embedding.length);

        } catch (Exception e) {
            log.error("Failed to extract features for alert ID {}: {}", message.getAlertId(), e.getMessage(), e);
        }
    }
}