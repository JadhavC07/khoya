package com.project.khoya.controller;

import com.project.khoya.config.CustomUserDetails;
import com.project.khoya.dto.*;
import com.project.khoya.entity.AlertStatus;
import com.project.khoya.entity.Role;
import com.project.khoya.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Alerts", description = "Missing person alert management APIs")
public class AlertController {

    private final AlertService alertService;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create new alert with image", description = "Create a new missing person alert with optional image upload to Cloudinary. Returns immediately while social media posting happens asynchronously.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Alert created successfully", content = @Content(schema = @Schema(implementation = SimpleAlertResponse.class))), @ApiResponse(responseCode = "400", description = "Invalid input or image"), @ApiResponse(responseCode = "401", description = "Unauthorized")})
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> createAlert(@Parameter(description = "Alert title", required = true) @RequestParam("title") String title,

                                         @Parameter(description = "Alert description", required = true) @RequestParam("description") String description,

                                         @Parameter(description = "Location where person was last seen", required = true) @RequestParam("location") String location,

                                         @Parameter(description = "Image of the missing person") @RequestParam(value = "image", required = false) MultipartFile image,

                                         @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            CreateAlertRequest request = new CreateAlertRequest();
            request.setTitle(title);
            request.setDescription(description);
            request.setLocation(location);

            SimpleAlertResponse response = alertService.createAlert(request, image, userDetails.getUser().getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IOException e) {
            log.error("Error creating alert with image", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping
    @Operation(summary = "Get all alerts", description = "Get paginated list of alerts with optional filtering by location and status")
    @ApiResponse(responseCode = "200", description = "Alerts retrieved successfully", content = @Content(schema = @Schema(implementation = AlertListResponse.class)))
    public ResponseEntity<AlertListResponse> getAllAlerts(@Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,

                                                          @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size,

                                                          @Parameter(description = "Filter by location (case-insensitive search)", example = "New York") @RequestParam(required = false) String location,

                                                          @Parameter(description = "Filter by alert status") @RequestParam(required = false) AlertStatus status) {

        AlertListResponse response = alertService.getAllAlerts(page, size, location, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alert by ID", description = "Get detailed information about a specific alert")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Alert found", content = @Content(schema = @Schema(implementation = AlertResponse.class))), @ApiResponse(responseCode = "404", description = "Alert not found")})
    public ResponseEntity<AlertResponse> getAlertById(@Parameter(description = "Alert ID", required = true) @PathVariable Long id) {

        AlertResponse response = alertService.getAlertById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update alert with image", description = "Update an existing alert with optional new image upload to Cloudinary (only by the owner)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Alert updated successfully", content = @Content(schema = @Schema(implementation = AlertResponse.class))), @ApiResponse(responseCode = "400", description = "Invalid input or image"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden - not alert owner"), @ApiResponse(responseCode = "404", description = "Alert not found")})
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateAlert(@Parameter(description = "Alert ID", required = true) @PathVariable Long id,

                                         @Parameter(description = "Alert title") @RequestParam(value = "title", required = false) String title,

                                         @Parameter(description = "Alert description") @RequestParam(value = "description", required = false) String description,

                                         @Parameter(description = "Location where person was last seen") @RequestParam(value = "location", required = false) String location,

                                         @Parameter(description = "Alert status") @RequestParam(value = "status", required = false) AlertStatus status,

                                         @Parameter(description = "New image of the missing person") @RequestParam(value = "image", required = false) MultipartFile image,

                                         @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            UpdateAlertRequest request = new UpdateAlertRequest();
            request.setTitle(title);
            request.setDescription(description);
            request.setLocation(location);
            request.setStatus(status);

            AlertResponse response = alertService.updateAlert(id, request, image, userDetails.getUser().getId());
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Error updating alert with image", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PutMapping("/{id}/found")
    @Operation(summary = "Mark alert as found", description = "Mark an alert as found (only by the owner)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Alert marked as found", content = @Content(schema = @Schema(implementation = AlertResponse.class))), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden - not alert owner"), @ApiResponse(responseCode = "404", description = "Alert not found")})
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<AlertResponse> markAsFound(@Parameter(description = "Alert ID", required = true) @PathVariable Long id, @RequestBody(required = false) FoundRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (request == null) {
            request = new FoundRequest();
        }

        AlertResponse response = alertService.markAsFound(id, request, userDetails.getUser().getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete alert", description = "Delete an alert and its Cloudinary image (owner or admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Alert deleted successfully"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "404", description = "Alert not found")})
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteAlert(@Parameter(description = "Alert ID", required = true) @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {

        boolean isAdmin = userDetails.getUser().getRole() == Role.ADMIN;
        alertService.deleteAlert(id, userDetails.getUser().getId(), isAdmin);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Alert deleted successfully");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my alerts", description = "Get all alerts posted by the current user", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "User alerts retrieved successfully")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<AlertResponse>> getMyAlerts(@AuthenticationPrincipal CustomUserDetails userDetails) {

        List<AlertResponse> response = alertService.getUserAlerts(userDetails.getUser().getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/report")
    @Operation(summary = "Report alert", description = "Report an alert (increment report count for admin review)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Alert reported successfully")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> reportAlert(@Parameter(description = "Alert ID", required = true) @PathVariable Long id) {

        alertService.incrementReportCount(id);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Alert reported successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/search/similar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Find visually similar alerts using AI", description = "Upload an image to find visually similar missing person alerts using ResNet50 deep learning model. " + "Returns alerts ranked by visual similarity with confidence scores. " + "Use this to check if a person has already been reported missing or to find potential matches.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Similar alerts found successfully", content = @Content(schema = @Schema(implementation = SimilarAlertResponse.class))), @ApiResponse(responseCode = "400", description = "Invalid image or request"), @ApiResponse(responseCode = "500", description = "Feature extraction failed")})
    public ResponseEntity<?> findSimilarAlerts(@Parameter(description = "Image to search for similar alerts", required = true) @RequestParam("image") MultipartFile image,

                                               @Parameter(description = "Number of top similar results to return", example = "10") @RequestParam(defaultValue = "10") int topK,

                                               @Parameter(description = "Minimum similarity threshold (0.0 to 1.0). Higher values return only very similar matches.", example = "0.5") @RequestParam(defaultValue = "0.5") double threshold) {

        try {
            if (image == null || image.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Image is required for similarity search");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (threshold < 0.0 || threshold > 1.0) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Threshold must be between 0.0 and 1.0");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            List<SimilarAlertResponse> similarAlerts = alertService.findSimilarAlerts(image, topK, threshold);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", similarAlerts.size());
            response.put("threshold", threshold);
            response.put("results", similarAlerts);

            if (similarAlerts.isEmpty()) {
                response.put("message", "No similar alerts found above the similarity threshold of " + (threshold * 100) + "%");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error finding similar alerts", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to find similar alerts: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping(value = "/search/similar/quick", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Quick similarity search with default settings", description = "Simplified endpoint for finding similar alerts with default threshold (60%) and top 5 results")
    @ApiResponse(responseCode = "200", description = "Similar alerts found")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> findSimilarAlertsQuick(@Parameter(description = "Image to search for", required = true) @RequestParam("image") MultipartFile image) {

        try {
            List<SimilarAlertResponse> similarAlerts = alertService.findSimilarAlerts(image, 5, 0.6);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", similarAlerts.size());
            response.put("results", similarAlerts);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in quick similarity search", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

}