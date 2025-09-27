//package com.project.khoya.service;
//
//import com.project.khoya.dto.*;
//import com.project.khoya.entity.AlertStatus;
//import com.project.khoya.entity.MissingAlert;
//import com.project.khoya.entity.User;
//import com.project.khoya.exception.AlertNotFoundException;
//import com.project.khoya.exception.UnauthorizedOperationException;
//import com.project.khoya.repository.MissingAlertRepository;
//import com.project.khoya.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
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
//
//    public AlertResponse createAlert(CreateAlertRequest request, Long userId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//
//
//        MissingAlert alert = new MissingAlert();
//        alert.setTitle(request.getTitle());
//        alert.setDescription(request.getDescription());
//        alert.setLocation(request.getLocation());
//        alert.setImageUrl(request.getImageUrl());
//        alert.setStatus(AlertStatus.ACTIVE);
//        alert.setPostedBy(user);
//
//        MissingAlert savedAlert = alertRepository.save(alert);
//        log.info("New alert created with ID: {} by user: {}", savedAlert.getId(), user.getEmail());
//
//
//
//        // 🔔 Send notification to all users
//        Map<String, String> data = Map.of(
//                "alertId", savedAlert.getId().toString(),
//                "type", "NEW_ALERT",
//                "location", savedAlert.getLocation() != null ? savedAlert.getLocation() : ""
//        );
//
//        firebaseMessagingService.sendPushNotificationToAllUsers(
//                "🚨 New Missing Person Alert",
//                savedAlert.getTitle() + " reported missing in " + savedAlert.getLocation(),
//                data
//        );
//
//        return mapToAlertResponse(savedAlert);
//    }
//
//    public AlertListResponse getAllAlerts(int page, int size, String location, AlertStatus status) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
//
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
//    public AlertResponse getAlertById(Long id) {
//        MissingAlert alert = alertRepository.findById(id)
//                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));
//
//        return mapToAlertResponse(alert);
//    }
//
//    public AlertResponse updateAlert(Long id, UpdateAlertRequest request, Long userId) {
//        MissingAlert alert = alertRepository.findById(id)
//                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));
//
//        // Check if user owns this alert
//        if (!alert.getPostedBy().getId().equals(userId)) {
//            throw new UnauthorizedOperationException("You can only update your own alerts");
//        }
//
//        // Update fields if provided
//        if (request.getTitle() != null) {
//            alert.setTitle(request.getTitle());
//        }
//        if (request.getDescription() != null) {
//            alert.setDescription(request.getDescription());
//        }
//        if (request.getLocation() != null) {
//            alert.setLocation(request.getLocation());
//        }
//        if (request.getImageUrl() != null) {
//            alert.setImageUrl(request.getImageUrl());
//        }
//        if (request.getStatus() != null) {
//            alert.setStatus(request.getStatus());
//            if (request.getStatus() == AlertStatus.FOUND && alert.getFoundAt() == null) {
//                alert.setFoundAt(LocalDateTime.now());
//            }
//        }
//
//        MissingAlert updatedAlert = alertRepository.save(alert);
//        log.info("Alert updated with ID: {} by user: {}", updatedAlert.getId(), userId);
//
//        return mapToAlertResponse(updatedAlert);
//    }
//
//    public AlertResponse markAsFound(Long id, FoundRequest request, Long userId) {
//        MissingAlert alert = alertRepository.findById(id)
//                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));
//
//        // Check if user owns this alert
//        if (!alert.getPostedBy().getId().equals(userId)) {
//            throw new UnauthorizedOperationException("You can only mark your own alerts as found");
//        }
//
//        alert.setStatus(AlertStatus.FOUND);
//        alert.setFoundAt(LocalDateTime.now());
//
//        // If found details provided, append to description
//        if (request.getFoundDetails() != null && !request.getFoundDetails().trim().isEmpty()) {
//            String updatedDescription = alert.getDescription() + "\n\nFound Details: " + request.getFoundDetails();
//            alert.setDescription(updatedDescription);
//        }
//
//        MissingAlert updatedAlert = alertRepository.save(alert);
//        log.info("Alert marked as found with ID: {} by user: {}", updatedAlert.getId(), userId);
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
//    public void deleteAlert(Long id, Long userId, boolean isAdmin) {
//        MissingAlert alert = alertRepository.findById(id)
//                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));
//
//        // Only alert owner or admin can delete
//        if (!isAdmin && !alert.getPostedBy().getId().equals(userId)) {
//            throw new UnauthorizedOperationException("You can only delete your own alerts");
//        }
//
//        alertRepository.delete(alert);
//        log.info("Alert deleted with ID: {} by user: {} (admin: {})", id, userId, isAdmin);
//    }
//
//    public List<AlertResponse> getUserAlerts(Long userId) {
//        List<MissingAlert> alerts = alertRepository.findByPostedByIdOrderByCreatedAtDesc(userId);
//
//        return alerts.stream()
//                .map(this::mapToAlertResponse)
//                .collect(Collectors.toList());
//    }
//
//    public void incrementReportCount(Long id) {
//        MissingAlert alert = alertRepository.findById(id)
//                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));
//
//        alert.setReportCount(alert.getReportCount() + 1);
//        alertRepository.save(alert);
//
//        log.info("Report count incremented for alert ID: {}", id);
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
//
//
//}
//

package com.project.khoya.service;

import com.project.khoya.dto.*;
import com.project.khoya.entity.AlertStatus;
import com.project.khoya.entity.MissingAlert;
import com.project.khoya.entity.User;
import com.project.khoya.exception.AlertNotFoundException;
import com.project.khoya.exception.UnauthorizedOperationException;
import com.project.khoya.repository.MissingAlertRepository;
import com.project.khoya.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final FileUploadService fileUploadService;

    public AlertResponse createAlert(CreateAlertRequest request, MultipartFile imageFile, Long userId) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MissingAlert alert = new MissingAlert();
        alert.setTitle(request.getTitle());
        alert.setDescription(request.getDescription());
        alert.setLocation(request.getLocation());
        alert.setStatus(AlertStatus.ACTIVE);
        alert.setPostedBy(user);

        // Handle image upload if provided
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imagePath = fileUploadService.uploadImage(imageFile);
                alert.setImageUrl(imagePath);
                log.info("Image uploaded for alert: {}", imagePath);
            } catch (IOException e) {
                log.error("Failed to upload image for alert", e);
                throw new IOException("Failed to upload image: " + e.getMessage());
            }
        }

        MissingAlert savedAlert = alertRepository.save(alert);
        log.info("New alert created with ID: {} by user: {}", savedAlert.getId(), user.getEmail());

        // 🔔 Send notification to all users
        Map<String, String> data = Map.of(
                "alertId", savedAlert.getId().toString(),
                "type", "NEW_ALERT",
                "location", savedAlert.getLocation() != null ? savedAlert.getLocation() : ""
        );

        firebaseMessagingService.sendPushNotificationToAllUsers(
                "🚨 New Missing Person Alert",
                savedAlert.getTitle() + " reported missing in " + savedAlert.getLocation(),
                data
        );

        return mapToAlertResponse(savedAlert);
    }

    public AlertListResponse getAllAlerts(int page, int size, String location, AlertStatus status) {
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

    public AlertResponse getAlertById(Long id) {
        MissingAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));

        return mapToAlertResponse(alert);
    }

    public AlertResponse updateAlert(Long id, UpdateAlertRequest request, MultipartFile imageFile, Long userId) throws IOException {
        MissingAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));

        // Check if user owns this alert
        if (!alert.getPostedBy().getId().equals(userId)) {
            throw new UnauthorizedOperationException("You can only update your own alerts");
        }

        // Store old image path for cleanup if needed
        String oldImagePath = alert.getImageUrl();

        // Update fields if provided
        if (request.getTitle() != null) {
            alert.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            alert.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            alert.setLocation(request.getLocation());
        }

        // Handle image update
        if (imageFile != null && !imageFile.isEmpty()) {
            // Delete old image if it exists and is an uploaded file
            if (oldImagePath != null && oldImagePath.startsWith("/uploads/")) {
                fileUploadService.deleteImage(oldImagePath);
            }

            try {
                String newImagePath = fileUploadService.uploadImage(imageFile);
                alert.setImageUrl(newImagePath);
                log.info("Image updated for alert ID: {} - new path: {}", id, newImagePath);
            } catch (IOException e) {
                log.error("Failed to update image for alert ID: {}", id, e);
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
        log.info("Alert updated with ID: {} by user: {}", updatedAlert.getId(), userId);

        return mapToAlertResponse(updatedAlert);
    }

    public AlertResponse markAsFound(Long id, FoundRequest request, Long userId) {
        MissingAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));

        // Check if user owns this alert
        if (!alert.getPostedBy().getId().equals(userId)) {
            throw new UnauthorizedOperationException("You can only mark your own alerts as found");
        }

        alert.setStatus(AlertStatus.FOUND);
        alert.setFoundAt(LocalDateTime.now());

        // If found details provided, append to description
        if (request.getFoundDetails() != null && !request.getFoundDetails().trim().isEmpty()) {
            String updatedDescription = alert.getDescription() + "\n\nFound Details: " + request.getFoundDetails();
            alert.setDescription(updatedDescription);
        }

        MissingAlert updatedAlert = alertRepository.save(alert);
        log.info("Alert marked as found with ID: {} by user: {}", updatedAlert.getId(), userId);

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

    public void deleteAlert(Long id, Long userId, boolean isAdmin) {
        MissingAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));

        // Only alert owner or admin can delete
        if (!isAdmin && !alert.getPostedBy().getId().equals(userId)) {
            throw new UnauthorizedOperationException("You can only delete your own alerts");
        }

        // Delete associated image if it exists and is an uploaded file
        if (alert.getImageUrl() != null && alert.getImageUrl().startsWith("/uploads/")) {
            fileUploadService.deleteImage(alert.getImageUrl());
        }

        alertRepository.delete(alert);
        log.info("Alert deleted with ID: {} by user: {} (admin: {})", id, userId, isAdmin);
    }

    public List<AlertResponse> getUserAlerts(Long userId) {
        List<MissingAlert> alerts = alertRepository.findByPostedByIdOrderByCreatedAtDesc(userId);

        return alerts.stream()
                .map(this::mapToAlertResponse)
                .collect(Collectors.toList());
    }

    public void incrementReportCount(Long id) {
        MissingAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + id));

        alert.setReportCount(alert.getReportCount() + 1);
        alertRepository.save(alert);

        log.info("Report count incremented for alert ID: {}", id);
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
