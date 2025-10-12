package com.project.khoya.controller;


import com.project.khoya.dto.AlertResponse;
import com.project.khoya.entity.MissingAlert;
import com.project.khoya.repository.MissingAlertRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Advanced search and filtering for alerts")
public class SearchController {

    private final MissingAlertRepository alertRepository;

    @GetMapping("/recent")
    @Operation(summary = "Get recent alerts", description = "Get alerts from the last specified number of days")
    public ResponseEntity<List<AlertResponse>> getRecentAlerts(@Parameter(description = "Number of days to look back", example = "7") @RequestParam(defaultValue = "7") int days) {

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<MissingAlert> alerts = alertRepository.findRecentAlerts(since);

        List<AlertResponse> response = alerts.stream().map(this::mapToAlertResponse).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active alerts", description = "Get all currently active alerts (not found or closed)")
    public ResponseEntity<List<AlertResponse>> getActiveAlerts() {
        List<MissingAlert> alerts = alertRepository.findActiveAlerts();

        List<AlertResponse> response = alerts.stream().map(this::mapToAlertResponse).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    private AlertResponse mapToAlertResponse(MissingAlert alert) {
        return AlertResponse.builder().id(alert.getId()).title(alert.getTitle()).description(alert.getDescription()).location(alert.getLocation()).imageUrl(alert.getImageUrl()).status(alert.getStatus()).reportCount(alert.getReportCount()).createdAt(alert.getCreatedAt()).updatedAt(alert.getUpdatedAt()).foundAt(alert.getFoundAt()).postedBy(AlertResponse.UserInfo.builder().id(alert.getPostedBy().getId()).name(alert.getPostedBy().getName()).email(alert.getPostedBy().getEmail()).build()).build();
    }
}


