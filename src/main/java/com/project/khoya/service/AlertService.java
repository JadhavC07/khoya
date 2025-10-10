//package com.project.khoya.service;
//
//import ai.djl.modality.cv.Image;
//import ai.djl.modality.cv.ImageFactory;
//import ai.djl.translate.TranslateException;
//import com.project.khoya.dto.*;
//import com.project.khoya.entity.AlertStatus;
//import com.project.khoya.entity.MissingAlert;
//import com.project.khoya.entity.User;
//import com.project.khoya.exception.AlertNotFoundException;
//import com.project.khoya.exception.UnauthorizedOperationException;
//import com.project.khoya.messaging.AlertMessageProducer;
//import com.project.khoya.repository.MissingAlertRepository;
//import com.project.khoya.repository.UserRepository;
//import com.project.khoya.service.Image.FeatureExtractionService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cache.annotation.CacheEvict;
//import org.springframework.cache.annotation.CachePut;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.cache.annotation.Caching;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//@Transactional
//public class AlertService {
//
//    private final MissingAlertRepository alertRepository;
//    private final UserRepository userRepository;
//    private final FirebaseMessagingService firebaseMessagingService;
//    private final CloudinaryService cloudinaryService;
//    private final AlertMessageProducer messageProducer;
//    private final FeatureExtractionService featureExtractionService;
//
//    @Caching(evict = {
//            @CacheEvict(value = "alerts", allEntries = true),
//            @CacheEvict(value = "userAlerts", key = "#userId")
//    })
//    public SimpleAlertResponse createAlert(CreateAlertRequest request, MultipartFile imageFile, Long userId) throws IOException {
//        log.info("Creating alert for user: {}", userId);
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        MissingAlert alert = new MissingAlert();
//        alert.setTitle(request.getTitle());
//        alert.setDescription(request.getDescription());
//        alert.setLocation(request.getLocation());
//        alert.setStatus(AlertStatus.ACTIVE);
//        alert.setPostedBy(user);
//
//        if (imageFile != null && !imageFile.isEmpty()) {
//            try {
//                String imageUrl = cloudinaryService.uploadImage(imageFile);
//                alert.setImageUrl(imageUrl);
//
//                // Extract features
//                Image image = ImageFactory.getInstance().fromInputStream(imageFile.getInputStream());
//                float[] embedding = featureExtractionService.extractFeatures(image);
//                alert.setImageEmbedding(floatArrayToBytes(embedding));
//
//            } catch (IOException | TranslateException e) {
//                log.error("Failed to process image", e);
//                throw new IOException("Failed to upload or extract features from image: " + e.getMessage());
//            }
//        }
//
//        MissingAlert savedAlert = alertRepository.save(alert);
//
//        // Send messages to RabbitMQ queues for async processing
//        AlertMessage alertMessage = AlertMessage.builder()
//                .alertId(savedAlert.getId())
//                .title(savedAlert.getTitle())
//                .description(savedAlert.getDescription())
//                .location(savedAlert.getLocation())
//                .imageUrl(savedAlert.getImageUrl())
//                .build();
//
//        if (savedAlert.getImageUrl() != null) {
//            messageProducer.sendSocialMediaMessage(alertMessage);
//        }
//        messageProducer.sendNotificationMessage(alertMessage);
//
//        return SimpleAlertResponse.builder()
//                .status("success")
//                .message("Alert created successfully and will be posted to social media shortly")
//                .alertId(savedAlert.getId())
//                .build();
//    }
//
//    private byte[] floatArrayToBytes(float[] array) {
//        ByteBuffer buffer = ByteBuffer.allocate(array.length * 4);
//        for (float value : array) {
//            buffer.putFloat(value);
//        }
//        return buffer.array();
//    }
//
//    private float[] bytesToFloatArray(byte[] bytes) {
//        ByteBuffer buffer = ByteBuffer.wrap(bytes);
//        float[] array = new float[bytes.length / 4];
//        for (int i = 0; i < array.length; i++) {
//            array[i] = buffer.getFloat();
//        }
//        return array;
//    }
//
//    public List<AlertResponse> findSimilarAlerts(MultipartFile newImageFile, int topK) throws Exception {
//        Image newImage = ImageFactory.getInstance().fromInputStream(newImageFile.getInputStream());
//        float[] newEmbedding = featureExtractionService.extractFeatures(newImage);
//
//        List<MissingAlert> allAlerts = alertRepository.findByStatus(AlertStatus.ACTIVE);
//        allAlerts.sort((a1, a2) -> {
//            float[] e1 = bytesToFloatArray(a1.getImageEmbedding());
//            float[] e2 = bytesToFloatArray(a2.getImageEmbedding());
//            double sim1 = cosineSimilarity(newEmbedding, e1);
//            double sim2 = cosineSimilarity(newEmbedding, e2);
//            return Double.compare(sim2, sim1);  // Descending order
//        });
//
//        List<MissingAlert> topAlerts = allAlerts.subList(0, Math.min(topK, allAlerts.size()));
//        return topAlerts.stream().map(this::mapToAlertResponse).collect(Collectors.toList());
//    }
//
//    private double cosineSimilarity(float[] vec1, float[] vec2) {
//        if (vec1.length != vec2.length) return 0.0;
//        double dot = 0.0;
//        double norm1 = 0.0;
//        double norm2 = 0.0;
//        for (int i = 0; i < vec1.length; i++) {
//            dot += vec1[i] * vec2[i];
//            norm1 += vec1[i] * vec1[i];
//            norm2 += vec2[i] * vec2[i];
//        }
//        if (norm1 == 0 || norm2 == 0) return 0.0;
//        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
//    }
//
//    @Cacheable(value = "alerts", key = "#page + '-' + #size + '-' + #location + '-' + #status")
//    public AlertListResponse getAllAlerts(int page, int size, String location, AlertStatus status) {
//        log.info("Fetching alerts from DB - page: {}, size: {}, location: {}, status: {}",
//                page, size, location, status);
//
//        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
//        Page<MissingAlert> alertPage;
//
//        if (location != null && !location.trim().isEmpty()) {
//            if (status != null) {
//                alertPage = alertRepository.findByLocationContainingIgnoreCaseAndStatus(
//                        location.trim(), status, pageable);
//            } else {
//                alertPage = alertRepository.findByLocationContainingIgnoreCase(
//                        location.trim(), pageable);
//            }
//        } else {
//            if (status != null) {
//                alertPage = alertRepository.findByStatus(status, pageable);
//            } else {
//                alertPage = alertRepository.findAll(pageable);
//            }
//        }
//
//        List<AlertResponse> alertResponses = alertPage.getContent().stream()
//                .map(this::mapToAlertResponse)
//                .collect(Collectors.toList());
//
//        return AlertListResponse.builder()
//                .alerts(alertResponses)
//                .page(alertPage.getNumber())
//                .size(alertPage.getSize())
//                .totalElements(alertPage.getTotalElements())
//                .totalPages(alertPage.getTotalPages())
//                .isFirst(alertPage.isFirst())
//                .isLast(alertPage.isLast())
//                .build();
//    }
//
//    @Cacheable(value = "alertById", key = "#id")
//    public AlertResponse getAlertById(Long id) {
//        log.info("Fetching alert by ID from DB: {}", id);
//        MissingAlert alert = alertRepository.findById(id)
//                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));
//
//        return mapToAlertResponse(alert);
//    }
//
//    @Caching(
//            put = @CachePut(value = "alertById", key = "#id"),
//            evict = {
//                    @CacheEvict(value = "alerts", allEntries = true),
//                    @CacheEvict(value = "userAlerts", key = "#userId")
//            }
//    )
//    public AlertResponse updateAlert(Long id, UpdateAlertRequest request, MultipartFile imageFile, Long userId) throws IOException {
//        log.info("Updating alert: {} by user: {}", id, userId);
//        MissingAlert alert = alertRepository.findById(id)
//                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));
//
//        if (!alert.getPostedBy().getId().equals(userId)) {
//            throw new UnauthorizedOperationException("You can only update your own alerts");
//        }
//
//        String oldImageUrl = alert.getImageUrl();
//
//        if (request.getTitle() != null) {
//            alert.setTitle(request.getTitle());
//        }
//        if (request.getDescription() != null) {
//            alert.setDescription(request.getDescription());
//        }
//        if (request.getLocation() != null) {
//            alert.setLocation(request.getLocation());
//        }
//
//        if (imageFile != null && !imageFile.isEmpty()) {
//            try {
//                String newImageUrl = cloudinaryService.uploadImage(imageFile);
//                alert.setImageUrl(newImageUrl);
//                if (oldImageUrl != null && oldImageUrl.contains("cloudinary.com")) {
//                    cloudinaryService.deleteImage(oldImageUrl);
//                }
//            } catch (IOException e) {
//                throw new IOException("Failed to update image: " + e.getMessage());
//            }
//        }
//
//        if (request.getStatus() != null) {
//            alert.setStatus(request.getStatus());
//            if (request.getStatus() == AlertStatus.FOUND && alert.getFoundAt() == null) {
//                alert.setFoundAt(LocalDateTime.now());
//            }
//        }
//
//        MissingAlert updatedAlert = alertRepository.save(alert);
//        return mapToAlertResponse(updatedAlert);
//    }
//
//    @Caching(
//            put = @CachePut(value = "alertById", key = "#id"),
//            evict = {
//                    @CacheEvict(value = "alerts", allEntries = true),
//                    @CacheEvict(value = "userAlerts", key = "#userId")
//            }
//    )
//    public AlertResponse markAsFound(Long id, FoundRequest request, Long userId) {
//        log.info("Marking alert as found: {} by user: {}", id, userId);
//        MissingAlert alert = alertRepository.findById(id)
//                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));
//
//        if (!alert.getPostedBy().getId().equals(userId)) {
//            throw new UnauthorizedOperationException("You can only mark your own alerts as found");
//        }
//
//        alert.setStatus(AlertStatus.FOUND);
//        alert.setFoundAt(LocalDateTime.now());
//
//        if (request.getFoundDetails() != null && !request.getFoundDetails().trim().isEmpty()) {
//            String updatedDescription = alert.getDescription() + "\n\nFound Details: " + request.getFoundDetails();
//            alert.setDescription(updatedDescription);
//        }
//
//        MissingAlert updatedAlert = alertRepository.save(alert);
//
//        Map<String, String> data = Map.of(
//                "alertId", updatedAlert.getId().toString(),
//                "type", "PERSON_FOUND"
//        );
//
//        firebaseMessagingService.sendPushNotificationToAllUsers(
//                "✅ Good News! Person Found",
//                updatedAlert.getTitle() + " has been found safely.",
//                data
//        );
//
//        return mapToAlertResponse(updatedAlert);
//    }
//
//    @Caching(evict = {
//            @CacheEvict(value = "alertById", key = "#id"),
//            @CacheEvict(value = "alerts", allEntries = true),
//            @CacheEvict(value = "userAlerts", key = "#userId")
//    })
//    public void deleteAlert(Long id, Long userId, boolean isAdmin) {
//        log.info("Deleting alert: {} by user: {}, isAdmin: {}", id, userId, isAdmin);
//        MissingAlert alert = alertRepository.findById(id)
//                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));
//
//        if (!isAdmin && !alert.getPostedBy().getId().equals(userId)) {
//            throw new UnauthorizedOperationException("You can only delete your own alerts");
//        }
//
//        if (alert.getImageUrl() != null && alert.getImageUrl().contains("cloudinary.com")) {
//            cloudinaryService.deleteImage(alert.getImageUrl());
//        }
//        alertRepository.delete(alert);
//    }
//
//    @Cacheable(value = "userAlerts", key = "#userId")
//    public List<AlertResponse> getUserAlerts(Long userId) {
//        log.info("Fetching user alerts from DB for user: {}", userId);
//        List<MissingAlert> alerts = alertRepository.findByPostedByIdOrderByCreatedAtDesc(userId);
//
//        return alerts.stream()
//                .map(this::mapToAlertResponse)
//                .collect(Collectors.toList());
//    }
//
//    @Caching(evict = {
//            @CacheEvict(value = "alertById", key = "#id"),
//            @CacheEvict(value = "alerts", allEntries = true)
//    })
//    public void incrementReportCount(Long id) {
//        log.info("Incrementing report count for alert: {}", id);
//        MissingAlert alert = alertRepository.findById(id)
//                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));
//
//        alert.setReportCount(alert.getReportCount() + 1);
//        alertRepository.save(alert);
//    }
//
//    private AlertResponse mapToAlertResponse(MissingAlert alert) {
//        return AlertResponse.builder()
//                .id(alert.getId())
//                .title(alert.getTitle())
//                .description(alert.getDescription())
//                .location(alert.getLocation())
//                .imageUrl(alert.getImageUrl())
//                .status(alert.getStatus())
//                .reportCount(alert.getReportCount())
//                .createdAt(alert.getCreatedAt())
//                .updatedAt(alert.getUpdatedAt())
//                .foundAt(alert.getFoundAt())
//                .postedBy(AlertResponse.UserInfo.builder()
//                        .id(alert.getPostedBy().getId())
//                        .name(alert.getPostedBy().getName())
//                        .email(alert.getPostedBy().getEmail())
//                        .build())
//                .build();
//    }
//}


package com.project.khoya.service;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.translate.TranslateException;
import com.project.khoya.dto.*;
import com.project.khoya.entity.AlertStatus;
import com.project.khoya.entity.MissingAlert;
import com.project.khoya.entity.User;
import com.project.khoya.exception.AlertNotFoundException;
import com.project.khoya.exception.UnauthorizedOperationException;
import com.project.khoya.messaging.AlertMessageProducer;
import com.project.khoya.repository.MissingAlertRepository;
import com.project.khoya.repository.UserRepository;
import com.project.khoya.service.Image.FeatureExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AlertService {

    private final MissingAlertRepository alertRepository;
    private final UserRepository userRepository;
    private final FirebaseMessagingService firebaseMessagingService;
    private final CloudinaryService cloudinaryService;
    private final AlertMessageProducer messageProducer;
    private final FeatureExtractionService featureExtractionService;

    /**
     * Create a new missing person alert with image and feature extraction.
     */
    @Caching(evict = {
            @CacheEvict(value = "alerts", allEntries = true),
            @CacheEvict(value = "userAlerts", key = "#userId")
    })
    public SimpleAlertResponse createAlert(CreateAlertRequest request, MultipartFile imageFile, Long userId) throws IOException {
        log.info("Creating alert for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MissingAlert alert = new MissingAlert();
        alert.setTitle(request.getTitle());
        alert.setDescription(request.getDescription());
        alert.setLocation(request.getLocation());
        alert.setStatus(AlertStatus.ACTIVE);
        alert.setPostedBy(user);

        // Process image if provided
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                // Upload image to Cloudinary
                String imageUrl = cloudinaryService.uploadImage(imageFile);
                alert.setImageUrl(imageUrl);
                log.info("Image uploaded to Cloudinary: {}", imageUrl);

                // Extract and store feature vector
                Image image = ImageFactory.getInstance().fromInputStream(imageFile.getInputStream());
                byte[] embedding = featureExtractionService.extractFeaturesAsBytes(image);
                alert.setImageEmbedding(embedding);

                log.info("Feature vector extracted (size: {} bytes, dimension: {})",
                        embedding.length, featureExtractionService.getFeatureDimension());

            } catch (IOException e) {
                log.error("Failed to upload image to Cloudinary", e);
                throw new IOException("Failed to upload image: " + e.getMessage(), e);
            } catch (TranslateException e) {
                log.error("Failed to extract features from image", e);
                throw new IOException("Failed to extract image features: " + e.getMessage(), e);
            }
        }

        MissingAlert savedAlert = alertRepository.save(alert);
        log.info("Alert created with ID: {}", savedAlert.getId());

        // Send async messages for social media posting and notifications
        AlertMessage alertMessage = AlertMessage.builder()
                .alertId(savedAlert.getId())
                .title(savedAlert.getTitle())
                .description(savedAlert.getDescription())
                .location(savedAlert.getLocation())
                .imageUrl(savedAlert.getImageUrl())
                .build();

        if (savedAlert.getImageUrl() != null) {
            messageProducer.sendSocialMediaMessage(alertMessage);
        }
        messageProducer.sendNotificationMessage(alertMessage);

        return SimpleAlertResponse.builder()
                .status("success")
                .message("Alert created successfully and will be posted to social media shortly")
                .alertId(savedAlert.getId())
                .build();
    }

    /**
     * Find visually similar alerts using image feature vectors.
     * Uses cosine similarity to rank alerts by visual similarity.
     *
     * @param newImageFile Image to search for
     * @param topK Number of top similar alerts to return
     * @param similarityThreshold Minimum similarity score (0.0 to 1.0)
     * @return List of similar alerts with similarity scores
     */
//    public List<SimilarAlertResponse> findSimilarAlerts(
//            MultipartFile newImageFile,
//            int topK,
//            double similarityThreshold) throws Exception {
//
//        log.info("Searching for similar alerts (topK: {}, threshold: {})", topK, similarityThreshold);
//
//        // Extract features from the query image
//        Image queryImage = ImageFactory.getInstance().fromInputStream(newImageFile.getInputStream());
//        float[] queryEmbedding = featureExtractionService.extractFeatures(queryImage);
//
//        // Get all active alerts with embeddings
//        List<MissingAlert> allAlerts = alertRepository.findByStatus(AlertStatus.ACTIVE);
//        List<MissingAlert> alertsWithEmbeddings = allAlerts.stream()
//                .filter(alert -> alert.getImageEmbedding() != null && alert.getImageEmbedding().length > 0)
//                .collect(Collectors.toList());
//
//        log.info("Comparing against {} alerts with embeddings", alertsWithEmbeddings.size());
//
//        // Calculate similarities and create response objects
//        List<SimilarAlertResponse> similarAlerts = new ArrayList<>();
//
//        for (MissingAlert alert : alertsWithEmbeddings) {
//            try {
//                float[] alertEmbedding = featureExtractionService.bytesToFloatArray(alert.getImageEmbedding());
//                double similarity = featureExtractionService.calculateCosineSimilarity(queryEmbedding, alertEmbedding);
//
//                // Only include alerts above the threshold
//                if (similarity >= similarityThreshold) {
//                    SimilarAlertResponse response = SimilarAlertResponse.builder()
//                            .alert(mapToAlertResponse(alert))
//                            .similarityScore(similarity)
//                            .build();
//                    similarAlerts.add(response);
//                }
//            } catch (Exception e) {
//                log.warn("Failed to calculate similarity for alert {}: {}", alert.getId(), e.getMessage());
//            }
//        }
//
//        // Sort by similarity (descending) and take top K
//        similarAlerts.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));
//        List<SimilarAlertResponse> topResults = similarAlerts.stream()
//                .limit(topK)
//                .collect(Collectors.toList());
//
//        log.info("Found {} similar alerts (filtered from {} total matches)",
//                topResults.size(), similarAlerts.size());
//
//        return topResults;
//    }

    public List<SimilarAlertResponse> findSimilarAlerts(
            MultipartFile newImageFile,
            int topK,
            double similarityThreshold) throws Exception {

        log.info("Searching for similar alerts (topK: {}, threshold: {})", topK, similarityThreshold);

        // Extract features from the query image
        Image queryImage = ImageFactory.getInstance().fromInputStream(newImageFile.getInputStream());
        float[] queryEmbedding = featureExtractionService.extractFeatures(queryImage);

        int expectedDimension = featureExtractionService.getFeatureDimension();
        log.info("Query embedding dimension: {}, expected: {}", queryEmbedding.length, expectedDimension);

        // Get all active alerts with embeddings
        List<MissingAlert> allAlerts = alertRepository.findByStatus(AlertStatus.ACTIVE);

        // Filter alerts that have the CORRECT dimension (2048)
        List<MissingAlert> alertsWithCorrectDimension = allAlerts.stream()
                .filter(alert -> {
                    if (alert.getImageEmbedding() == null || alert.getImageEmbedding().length == 0) {
                        return false;
                    }

                    int alertDimension = alert.getImageEmbedding().length / Float.BYTES;
                    boolean isCorrect = alertDimension == expectedDimension;

                    if (!isCorrect) {
                        log.debug("Skipping alert {} - wrong dimension: {} (expected {})",
                                alert.getId(), alertDimension, expectedDimension);
                    }

                    return isCorrect;
                })
                .toList();

        log.info("Comparing against {} alerts with correct dimension ({})",
                alertsWithCorrectDimension.size(), expectedDimension);

        if (alertsWithCorrectDimension.isEmpty()) {
            log.warn("No alerts found with {}-dimensional embeddings. Create new alerts to test similarity search.",
                    expectedDimension);
        }

        // Calculate similarities
        List<SimilarAlertResponse> similarAlerts = new ArrayList<>();

        for (MissingAlert alert : alertsWithCorrectDimension) {
            try {
                float[] alertEmbedding = featureExtractionService.bytesToFloatArray(alert.getImageEmbedding());
                double similarity = featureExtractionService.calculateCosineSimilarity(queryEmbedding, alertEmbedding);

                if (similarity >= similarityThreshold) {
                    SimilarAlertResponse response = SimilarAlertResponse.builder()
                            .alert(mapToAlertResponse(alert))
                            .similarityScore(similarity)
                            .build();
                    similarAlerts.add(response);
                }
            } catch (Exception e) {
                log.warn("Failed to calculate similarity for alert {}: {}", alert.getId(), e.getMessage());
            }
        }

        // Sort by similarity (descending) and take top K
        similarAlerts.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));
        List<SimilarAlertResponse> topResults = similarAlerts.stream()
                .limit(topK)
                .collect(Collectors.toList());

        log.info("Found {} similar alerts (filtered from {} total matches)",
                topResults.size(), similarAlerts.size());

        return topResults;
    }

    /**
     * Convenience method with default threshold.
     */
    public List<SimilarAlertResponse> findSimilarAlerts(MultipartFile newImageFile, int topK) throws Exception {
        return findSimilarAlerts(newImageFile, topK, 0.5); // Default 50% similarity threshold
    }

    @Cacheable(value = "alerts", key = "#page + '-' + #size + '-' + #location + '-' + #status")
    public AlertListResponse getAllAlerts(int page, int size, String location, AlertStatus status) {
        log.info("Fetching alerts - page: {}, size: {}, location: {}, status: {}", page, size, location, status);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<MissingAlert> alertPage;

        if (location != null && !location.trim().isEmpty()) {
            if (status != null) {
                alertPage = alertRepository.findByLocationContainingIgnoreCaseAndStatus(location.trim(), status, pageable);
            } else {
                alertPage = alertRepository.findByLocationContainingIgnoreCase(location.trim(), pageable);
            }
        } else {
            if (status != null) {
                alertPage = alertRepository.findByStatus(status, pageable);
            } else {
                alertPage = alertRepository.findAll(pageable);
            }
        }

        List<AlertResponse> alertResponses = alertPage.getContent().stream()
                .map(this::mapToAlertResponse)
                .collect(Collectors.toList());

        return AlertListResponse.builder()
                .alerts(alertResponses)
                .page(alertPage.getNumber())
                .size(alertPage.getSize())
                .totalElements(alertPage.getTotalElements())
                .totalPages(alertPage.getTotalPages())
                .isFirst(alertPage.isFirst())
                .isLast(alertPage.isLast())
                .build();
    }

    @Cacheable(value = "alertById", key = "#id")
    public AlertResponse getAlertById(Long id) {
        log.info("Fetching alert by ID: {}", id);
        MissingAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));
        return mapToAlertResponse(alert);
    }

    @Caching(
            put = @CachePut(value = "alertById", key = "#id"),
            evict = {
                    @CacheEvict(value = "alerts", allEntries = true),
                    @CacheEvict(value = "userAlerts", key = "#userId")
            }
    )
    public AlertResponse updateAlert(Long id, UpdateAlertRequest request, MultipartFile imageFile, Long userId) throws IOException {
        log.info("Updating alert: {} by user: {}", id, userId);

        MissingAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));

        if (!alert.getPostedBy().getId().equals(userId)) {
            throw new UnauthorizedOperationException("You can only update your own alerts");
        }

        String oldImageUrl = alert.getImageUrl();

        // Update basic fields
        if (request.getTitle() != null) alert.setTitle(request.getTitle());
        if (request.getDescription() != null) alert.setDescription(request.getDescription());
        if (request.getLocation() != null) alert.setLocation(request.getLocation());

        // Update image and extract new features
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String newImageUrl = cloudinaryService.uploadImage(imageFile);
                alert.setImageUrl(newImageUrl);

                // Extract new features
                Image image = ImageFactory.getInstance().fromInputStream(imageFile.getInputStream());
                byte[] embedding = featureExtractionService.extractFeaturesAsBytes(image);
                alert.setImageEmbedding(embedding);

                // Delete old image from Cloudinary
                if (oldImageUrl != null && oldImageUrl.contains("cloudinary.com")) {
                    cloudinaryService.deleteImage(oldImageUrl);
                }

                log.info("Image and features updated for alert {}", id);

            } catch (TranslateException e) {
                throw new IOException("Failed to extract features from new image: " + e.getMessage(), e);
            }
        }

        // Update status
        if (request.getStatus() != null) {
            alert.setStatus(request.getStatus());
            if (request.getStatus() == AlertStatus.FOUND && alert.getFoundAt() == null) {
                alert.setFoundAt(LocalDateTime.now());
            }
        }

        MissingAlert updatedAlert = alertRepository.save(alert);
        return mapToAlertResponse(updatedAlert);
    }

    @Caching(
            put = @CachePut(value = "alertById", key = "#id"),
            evict = {
                    @CacheEvict(value = "alerts", allEntries = true),
                    @CacheEvict(value = "userAlerts", key = "#userId")
            }
    )
    public AlertResponse markAsFound(Long id, FoundRequest request, Long userId) {
        log.info("Marking alert as found: {} by user: {}", id, userId);

        MissingAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));

        if (!alert.getPostedBy().getId().equals(userId)) {
            throw new UnauthorizedOperationException("You can only mark your own alerts as found");
        }

        alert.setStatus(AlertStatus.FOUND);
        alert.setFoundAt(LocalDateTime.now());

        if (request.getFoundDetails() != null && !request.getFoundDetails().trim().isEmpty()) {
            String updatedDescription = alert.getDescription() + "\n\nFound Details: " + request.getFoundDetails();
            alert.setDescription(updatedDescription);
        }

        MissingAlert updatedAlert = alertRepository.save(alert);

        // Send notification
        Map<String, String> data = Map.of(
                "alertId", updatedAlert.getId().toString(),
                "type", "PERSON_FOUND"
        );

        firebaseMessagingService.sendPushNotificationToAllUsers(
                "✅ Good News! Person Found",
                updatedAlert.getTitle() + " has been found safely.",
                data
        );

        return mapToAlertResponse(updatedAlert);
    }

    @Caching(evict = {
            @CacheEvict(value = "alertById", key = "#id"),
            @CacheEvict(value = "alerts", allEntries = true),
            @CacheEvict(value = "userAlerts", key = "#userId")
    })
    public void deleteAlert(Long id, Long userId, boolean isAdmin) {
        log.info("Deleting alert: {} by user: {}, isAdmin: {}", id, userId, isAdmin);

        MissingAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));

        if (!isAdmin && !alert.getPostedBy().getId().equals(userId)) {
            throw new UnauthorizedOperationException("You can only delete your own alerts");
        }

        if (alert.getImageUrl() != null && alert.getImageUrl().contains("cloudinary.com")) {
            cloudinaryService.deleteImage(alert.getImageUrl());
        }

        alertRepository.delete(alert);
        log.info("Alert {} deleted successfully", id);
    }

    @Cacheable(value = "userAlerts", key = "#userId")
    public List<AlertResponse> getUserAlerts(Long userId) {
        log.info("Fetching user alerts for user: {}", userId);
        List<MissingAlert> alerts = alertRepository.findByPostedByIdOrderByCreatedAtDesc(userId);
        return alerts.stream()
                .map(this::mapToAlertResponse)
                .collect(Collectors.toList());
    }

    @Caching(evict = {
            @CacheEvict(value = "alertById", key = "#id"),
            @CacheEvict(value = "alerts", allEntries = true)
    })
    public void incrementReportCount(Long id) {
        log.info("Incrementing report count for alert: {}", id);
        MissingAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));
        alert.setReportCount(alert.getReportCount() + 1);
        alertRepository.save(alert);
    }

    private AlertResponse mapToAlertResponse(MissingAlert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .title(alert.getTitle())
                .description(alert.getDescription())
                .location(alert.getLocation())
                .imageUrl(alert.getImageUrl())
                .status(alert.getStatus())
                .reportCount(alert.getReportCount())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .foundAt(alert.getFoundAt())
                .postedBy(AlertResponse.UserInfo.builder()
                        .id(alert.getPostedBy().getId())
                        .name(alert.getPostedBy().getName())
                        .email(alert.getPostedBy().getEmail())
                        .build())
                .build();
    }
}