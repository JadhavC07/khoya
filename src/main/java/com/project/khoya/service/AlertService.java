package com.project.khoya.service;

import com.project.khoya.dto.*;
import com.project.khoya.entity.AlertStatus;
import com.project.khoya.entity.MissingAlert;
import com.project.khoya.entity.User;
import com.project.khoya.exception.AlertNotFoundException;
import com.project.khoya.exception.UnauthorizedOperationException;
import com.project.khoya.messaging.AlertMessageProducer;
import com.project.khoya.repository.MissingAlertRepository;
import com.project.khoya.repository.UserRepository;
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

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imageUrl = cloudinaryService.uploadImage(imageFile);
                alert.setImageUrl(imageUrl);
            } catch (IOException e) {
                throw new IOException("Failed to upload image: " + e.getMessage());
            }
        }

        MissingAlert savedAlert = alertRepository.save(alert);

        // Send messages to RabbitMQ queues for async processing
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

    @Cacheable(value = "alerts", key = "#page + '-' + #size + '-' + #location + '-' + #status")
    public AlertListResponse getAllAlerts(int page, int size, String location, AlertStatus status) {
        log.info("Fetching alerts from DB - page: {}, size: {}, location: {}, status: {}",
                page, size, location, status);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<MissingAlert> alertPage;

        if (location != null && !location.trim().isEmpty()) {
            if (status != null) {
                alertPage = alertRepository.findByLocationContainingIgnoreCaseAndStatus(
                        location.trim(), status, pageable);
            } else {
                alertPage = alertRepository.findByLocationContainingIgnoreCase(
                        location.trim(), pageable);
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
        log.info("Fetching alert by ID from DB: {}", id);
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

        if (request.getTitle() != null) {
            alert.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            alert.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            alert.setLocation(request.getLocation());
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String newImageUrl = cloudinaryService.uploadImage(imageFile);
                alert.setImageUrl(newImageUrl);
                if (oldImageUrl != null && oldImageUrl.contains("cloudinary.com")) {
                    cloudinaryService.deleteImage(oldImageUrl);
                }
            } catch (IOException e) {
                throw new IOException("Failed to update image: " + e.getMessage());
            }
        }

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
    }

    @Cacheable(value = "userAlerts", key = "#userId")
    public List<AlertResponse> getUserAlerts(Long userId) {
        log.info("Fetching user alerts from DB for user: {}", userId);
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