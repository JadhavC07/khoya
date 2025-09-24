package com.project.khoya.controller;

import com.project.khoya.config.CustomUserDetails;
import com.project.khoya.dto.ReportAlertRequest;
import com.project.khoya.dto.ReportResponse;
import com.project.khoya.service.AlertReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Alert reporting and moderation APIs")
public class ReportingController {

    private final AlertReportingService reportingService;

    @PostMapping("/alert/{alertId}")
    @Operation(
            summary = "Report an alert",
            description = "Report an alert as fake or inappropriate",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Alert reported successfully",
                    content = @Content(schema = @Schema(implementation = ReportResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input or duplicate report"),
            @ApiResponse(responseCode = "404", description = "Alert not found")
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> reportAlert(
            @Parameter(description = "Alert ID to report", required = true)
            @PathVariable Long alertId,
            @Valid @RequestBody ReportAlertRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ReportResponse response = reportingService.reportAlert(alertId, request, userDetails.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/alert/{alertId}")
    @Operation(
            summary = "Get reports for an alert",
            description = "Get all reports for a specific alert (admin only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReportResponse>> getAlertReports(
            @Parameter(description = "Alert ID", required = true)
            @PathVariable Long alertId) {

        List<ReportResponse> reports = reportingService.getAlertReports(alertId);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/my")
    @Operation(
            summary = "Get my reports",
            description = "Get all reports made by the current user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<ReportResponse>> getMyReports(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<ReportResponse> reports = reportingService.getUserReports(userDetails.getUser().getId());
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/pending")
    @Operation(
            summary = "Get pending reports",
            description = "Get pending reports for admin review",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReportResponse>> getPendingReports(
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        List<ReportResponse> reports = reportingService.getPendingReports(page, size);
        return ResponseEntity.ok(reports);
    }
}
