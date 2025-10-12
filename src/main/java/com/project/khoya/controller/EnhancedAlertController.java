package com.project.khoya.controller;

import com.project.khoya.config.CustomUserDetails;
import com.project.khoya.dto.EnhancedAlertListResponse;
import com.project.khoya.dto.EnhancedAlertResponse;
import com.project.khoya.entity.AlertStatus;
import com.project.khoya.service.EnhancedAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts-social")
@RequiredArgsConstructor
@Tag(name = "Enhanced Alerts", description = "Alert APIs with social interaction data")
public class EnhancedAlertController {

    private final EnhancedAlertService enhancedAlertService;

    @GetMapping
    @Operation(summary = "Get alerts with social data", description = "Get paginated list of alerts with vote counts, comment counts, and social interaction data")
    @ApiResponse(responseCode = "200", description = "Alerts retrieved successfully", content = @Content(schema = @Schema(implementation = EnhancedAlertListResponse.class)))
    public ResponseEntity<EnhancedAlertListResponse> getAlertsWithSocialData(@Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,

                                                                             @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size,

                                                                             @Parameter(description = "Filter by location (case-insensitive search)", example = "New York") @RequestParam(required = false) String location,

                                                                             @Parameter(description = "Filter by alert status") @RequestParam(required = false) AlertStatus status,

                                                                             @Parameter(description = "Sort by: recent, score, comments, engagement, oldest", example = "recent") @RequestParam(defaultValue = "recent") String sortBy,

                                                                             @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long currentUserId = userDetails != null ? userDetails.getUser().getId() : null;
        EnhancedAlertListResponse response = enhancedAlertService.getAlertsWithSocialData(page, size, location, status, sortBy, currentUserId);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get alert with social data", description = "Get detailed information about a specific alert including social interaction data")
    @ApiResponse(responseCode = "200", description = "Alert found with social data", content = @Content(schema = @Schema(implementation = EnhancedAlertResponse.class)))
    public ResponseEntity<EnhancedAlertResponse> getAlertWithSocialData(@Parameter(description = "Alert ID", required = true) @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long currentUserId = userDetails != null ? userDetails.getUser().getId() : null;
        EnhancedAlertResponse response = enhancedAlertService.getAlertWithSocialData(id, currentUserId);

        return ResponseEntity.ok(response);
    }
}

