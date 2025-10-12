package com.project.khoya.controller;

import com.project.khoya.dto.AlertResponse;
import com.project.khoya.entity.AlertStatus;
import com.project.khoya.repository.MissingAlertRepository;
import com.project.khoya.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/alerts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Alerts", description = "Admin operations for alert management")
public class AdminAlertController {

    private final AlertService alertService;
    private final MissingAlertRepository alertRepository;

    @GetMapping("/stats")
    @Operation(summary = "Get alert statistics", description = "Get comprehensive statistics about alerts", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, Object>> getAlertStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalAlerts", alertRepository.count());
        stats.put("activeAlerts", alertRepository.countByStatus(AlertStatus.ACTIVE));
        stats.put("foundAlerts", alertRepository.countByStatus(AlertStatus.FOUND));
        stats.put("closedAlerts", alertRepository.countByStatus(AlertStatus.CLOSED));
        stats.put("underReviewAlerts", alertRepository.countByStatus(AlertStatus.UNDER_REVIEW));


        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        stats.put("recentAlerts", alertRepository.findRecentAlerts(weekAgo).size());


        stats.put("highReportAlerts", alertRepository.findAlertsWithHighReportCount(5).size());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/high-reports")
    @Operation(summary = "Get alerts with high report count", description = "Get alerts that have been reported multiple times for admin review", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, Object>> getHighReportAlerts(@RequestParam(defaultValue = "5") int threshold) {

        var alerts = alertRepository.findAlertsWithHighReportCount(threshold);
        var alertResponses = alerts.stream().map(alert -> AlertResponse.builder().id(alert.getId()).title(alert.getTitle()).description(alert.getDescription()).location(alert.getLocation()).status(alert.getStatus()).reportCount(alert.getReportCount()).createdAt(alert.getCreatedAt()).postedBy(AlertResponse.UserInfo.builder().id(alert.getPostedBy().getId()).name(alert.getPostedBy().getName()).email(alert.getPostedBy().getEmail()).build()).build()).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("alerts", alertResponses);
        response.put("threshold", threshold);
        response.put("count", alertResponses.size());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update alert status", description = "Update the status of any alert (admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AlertResponse> updateAlertStatus(@PathVariable Long id, @RequestParam AlertStatus status) {

        var alert = alertRepository.findById(id).orElseThrow(() -> new RuntimeException("Alert not found"));

        alert.setStatus(status);
        if (status == AlertStatus.FOUND && alert.getFoundAt() == null) {
            alert.setFoundAt(LocalDateTime.now());
        }

        var savedAlert = alertRepository.save(alert);

        return ResponseEntity.ok(AlertResponse.builder().id(savedAlert.getId()).title(savedAlert.getTitle()).description(savedAlert.getDescription()).location(savedAlert.getLocation()).imageUrl(savedAlert.getImageUrl()).status(savedAlert.getStatus()).reportCount(savedAlert.getReportCount()).createdAt(savedAlert.getCreatedAt()).updatedAt(savedAlert.getUpdatedAt()).foundAt(savedAlert.getFoundAt()).postedBy(AlertResponse.UserInfo.builder().id(savedAlert.getPostedBy().getId()).name(savedAlert.getPostedBy().getName()).email(savedAlert.getPostedBy().getEmail()).build()).build());
    }
}
