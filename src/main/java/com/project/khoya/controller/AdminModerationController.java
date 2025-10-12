package com.project.khoya.controller;

import com.project.khoya.config.CustomUserDetails;
import com.project.khoya.dto.ReportResponse;
import com.project.khoya.entity.ReportStatus;
import com.project.khoya.repository.AlertReportRepository;
import com.project.khoya.repository.MissingAlertRepository;
import com.project.khoya.service.AlertReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Moderation", description = "Admin moderation and reporting management")
public class AdminModerationController {

    private final AlertReportingService reportingService;
    private final AlertReportRepository reportRepository;
    private final MissingAlertRepository alertRepository;

    @GetMapping("/stats")
    @Operation(summary = "Get moderation statistics", description = "Get comprehensive moderation statistics", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, Object>> getModerationStats() {
        Map<String, Object> stats = new HashMap<>();

        // Report statistics
        stats.put("totalReports", reportRepository.count());
        stats.put("pendingReports", reportRepository.countByStatus(ReportStatus.PENDING));
        stats.put("validReports", reportRepository.countByStatus(ReportStatus.VALID));
        stats.put("invalidReports", reportRepository.countByStatus(ReportStatus.INVALID));
        stats.put("dismissedReports", reportRepository.countByStatus(ReportStatus.DISMISSED));

        // Alert moderation statistics
        stats.put("flaggedAlerts", alertRepository.countByIsFlagged(true));
        stats.put("autoDeletedAlerts", alertRepository.countByAutoDeleted(true));

        // Recent reports (last 24 hours)
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        stats.put("recentReports", reportRepository.findRecentReports(yesterday).size());

        // High-priority alerts (flagged or high report count)
        stats.put("highPriorityAlerts", alertRepository.findAlertsNeedingAttention().size());

        return ResponseEntity.ok(stats);
    }

    @PutMapping("/reports/{reportId}/review")
    @Operation(summary = "Review a report", description = "Review a report and set its status", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ReportResponse> reviewReport(@Parameter(description = "Report ID", required = true) @PathVariable Long reportId,

                                                       @Parameter(description = "Report status decision", required = true) @RequestParam ReportStatus status,

                                                       @Parameter(description = "Admin notes") @RequestParam(required = false) @Size(max = 1000) String adminNotes,

                                                       @AuthenticationPrincipal CustomUserDetails userDetails) {

        ReportResponse response = reportingService.reviewReport(reportId, status, adminNotes, userDetails.getUser().getId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/alerts/flagged")
    @Operation(summary = "Get flagged alerts", description = "Get all flagged alerts for review", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, Object>> getFlaggedAlerts(@Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page, @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        var flaggedAlerts = alertRepository.findFlaggedAlerts(org.springframework.data.domain.PageRequest.of(page, size));

        var alertResponses = flaggedAlerts.getContent().stream().map(alert -> Map.of("id", alert.getId(), "title", alert.getTitle(), "reportCount", alert.getReportCount(), "flaggedAt", alert.getFlaggedAt(), "flaggedReason", alert.getFlaggedReason() != null ? alert.getFlaggedReason() : "", "status", alert.getStatus(), "autoDeleted", alert.isAutoDeleted(), "postedBy", Map.of("id", alert.getPostedBy().getId(), "name", alert.getPostedBy().getName(), "email", alert.getPostedBy().getEmail()))).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("alerts", alertResponses);
        response.put("page", flaggedAlerts.getNumber());
        response.put("totalElements", flaggedAlerts.getTotalElements());
        response.put("totalPages", flaggedAlerts.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/alerts/{alertId}/unflag")
    @Operation(summary = "Unflag an alert", description = "Remove flag from an alert after review", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> unflagAlert(@Parameter(description = "Alert ID", required = true) @PathVariable Long alertId, @Parameter(description = "Reason for unflagging") @RequestParam(required = false) String reason) {

        var alert = alertRepository.findById(alertId).orElseThrow(() -> new RuntimeException("Alert not found"));

        alert.setFlagged(false);
        alert.setFlaggedAt(null);
        alert.setFlaggedReason(reason != null ? "Unflagged: " + reason : "Unflagged by admin");

        alertRepository.save(alert);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Alert unflagged successfully");

        return ResponseEntity.ok(response);
    }
}
