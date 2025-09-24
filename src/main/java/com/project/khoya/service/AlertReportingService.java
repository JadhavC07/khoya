package com.project.khoya.service;

import com.project.khoya.dto.ReportAlertRequest;
import com.project.khoya.dto.ReportResponse;
import com.project.khoya.entity.*;
import com.project.khoya.exception.AlertNotFoundException;
import com.project.khoya.exception.DuplicateReportException;
import com.project.khoya.exception.SelfReportException;
import com.project.khoya.repository.AlertReportRepository;
import com.project.khoya.repository.MissingAlertRepository;
import com.project.khoya.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AlertReportingService {

    private final AlertReportRepository reportRepository;
    private final MissingAlertRepository alertRepository;
    private final UserRepository userRepository;

    @Value("${app.auto-moderation.flag-threshold:5}")
    private int flagThreshold;

    @Value("${app.auto-moderation.delete-threshold:10}")
    private int deleteThreshold;

    @Value("${app.auto-moderation.enabled:true}")
    private boolean autoModerationEnabled;

    public ReportResponse reportAlert(Long alertId, ReportAlertRequest request, Long userId) {
        // Get alert
        MissingAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + alertId));

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent self-reporting
        if (alert.getPostedBy().getId().equals(userId)) {
            throw new SelfReportException("You cannot report your own alert");
        }

        // Check if user already reported this alert
        if (reportRepository.existsByAlertIdAndReportedById(alertId, userId)) {
            throw new DuplicateReportException("You have already reported this alert");
        }

        // Create report
        AlertReport report = new AlertReport();
        report.setAlert(alert);
        report.setReportedBy(user);
        report.setReason(request.getReason());
        report.setAdditionalDetails(request.getAdditionalDetails());
        report.setStatus(ReportStatus.PENDING);

        AlertReport savedReport = reportRepository.save(report);

        // Update alert report count
        alert.setReportCount(alert.getReportCount() + 1);
        alertRepository.save(alert);

        log.info("Alert {} reported by user {} for reason: {}", alertId, userId, request.getReason());

        // Trigger auto-moderation if enabled
        if (autoModerationEnabled) {
            checkAndApplyAutoModeration(alert);
        }

        return mapToReportResponse(savedReport);
    }

    private void checkAndApplyAutoModeration(MissingAlert alert) {
        int reportCount = alert.getReportCount();

        // Auto-flag if threshold reached
        if (reportCount >= flagThreshold && !alert.isFlagged()) {
            alert.setFlagged(true);
            alert.setFlaggedAt(LocalDateTime.now());
            alert.setFlaggedReason("Auto-flagged due to high report count (" + reportCount + " reports)");
            log.warn("Alert {} auto-flagged due to {} reports", alert.getId(), reportCount);
        }

        // Auto-delete if threshold reached
        if (reportCount >= deleteThreshold && !alert.isAutoDeleted()) {
            alert.setAutoDeleted(true);
            alert.setAutoDeletedAt(LocalDateTime.now());
            alert.setStatus(AlertStatus.CLOSED);
            log.warn("Alert {} auto-deleted due to {} reports", alert.getId(), reportCount);
        }

        alertRepository.save(alert);
    }

    public List<ReportResponse> getAlertReports(Long alertId) {
        List<AlertReport> reports = reportRepository.findByAlertIdOrderByCreatedAtDesc(alertId);
        return reports.stream()
                .map(this::mapToReportResponse)
                .collect(Collectors.toList());
    }

    public List<ReportResponse> getUserReports(Long userId) {
        List<AlertReport> reports = reportRepository.findByReportedByIdOrderByCreatedAtDesc(userId);
        return reports.stream()
                .map(this::mapToReportResponse)
                .collect(Collectors.toList());
    }

    public List<ReportResponse> getPendingReports(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AlertReport> reportPage = reportRepository.findByStatusOrderByCreatedAtDesc(
                ReportStatus.PENDING, pageable);

        return reportPage.getContent().stream()
                .map(this::mapToReportResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReportResponse reviewReport(Long reportId, ReportStatus status, String adminNotes, Long adminId) {
        AlertReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        report.setStatus(status);
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewedBy(admin);
        report.setAdminNotes(adminNotes);

        AlertReport savedReport = reportRepository.save(report);

        log.info("Report {} reviewed by admin {} with status: {}", reportId, adminId, status);

        // If report is marked as valid, take action on the alert
        if (status == ReportStatus.VALID) {
            takeActionOnReportedAlert(report.getAlert(), adminNotes);
        }

        return mapToReportResponse(savedReport);
    }

    private void takeActionOnReportedAlert(MissingAlert alert, String reason) {
        // Flag the alert and optionally close it
        alert.setFlagged(true);
        alert.setFlaggedAt(LocalDateTime.now());
        alert.setFlaggedReason("Admin action: " + reason);
        alert.setStatus(AlertStatus.UNDER_REVIEW);

        alertRepository.save(alert);
        log.info("Admin action taken on alert {}: flagged and under review", alert.getId());
    }

    private ReportResponse mapToReportResponse(AlertReport report) {
        return ReportResponse.builder()
                .id(report.getId())
                .alertId(report.getAlert().getId())
                .alertTitle(report.getAlert().getTitle())
                .reason(report.getReason())
                .additionalDetails(report.getAdditionalDetails())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .reviewedAt(report.getReviewedAt())
                .adminNotes(report.getAdminNotes())
                .reportedBy(ReportResponse.UserInfo.builder()
                        .id(report.getReportedBy().getId())
                        .name(report.getReportedBy().getName())
                        .email(report.getReportedBy().getEmail())
                        .build())
                .reviewedBy(report.getReviewedBy() != null ?
                        ReportResponse.UserInfo.builder()
                                .id(report.getReviewedBy().getId())
                                .name(report.getReviewedBy().getName())
                                .email(report.getReviewedBy().getEmail())
                                .build() : null)
                .build();
    }
}