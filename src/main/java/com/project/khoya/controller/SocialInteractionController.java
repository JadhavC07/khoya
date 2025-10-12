package com.project.khoya.controller;

import com.project.khoya.config.CustomUserDetails;
import com.project.khoya.dto.CommentResponse;
import com.project.khoya.dto.CreateCommentRequest;
import com.project.khoya.dto.UpdateCommentRequest;
import com.project.khoya.dto.VoteRequest;
import com.project.khoya.entity.Role;
import com.project.khoya.service.SocialInteractionService;
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
@RequestMapping("/api/social")
@RequiredArgsConstructor
@Tag(name = "Social Interactions", description = "Comments and voting APIs")
public class SocialInteractionController {

    private final SocialInteractionService socialService;

    // COMMENT ENDPOINTS

    @PostMapping("/alerts/{alertId}/comments")
    @Operation(summary = "Add comment to alert", description = "Add a comment or reply to an alert", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Comment created successfully", content = @Content(schema = @Schema(implementation = CommentResponse.class))), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "404", description = "Alert not found")})
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CommentResponse> addComment(@Parameter(description = "Alert ID", required = true) @PathVariable Long alertId, @Valid @RequestBody CreateCommentRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {

        CommentResponse response = socialService.createComment(alertId, request, userDetails.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/alerts/{alertId}/comments")
    @Operation(summary = "Get alert comments", description = "Get all comments for an alert with optional sorting")
    @ApiResponse(responseCode = "200", description = "Comments retrieved successfully")
    public ResponseEntity<List<CommentResponse>> getAlertComments(@Parameter(description = "Alert ID", required = true) @PathVariable Long alertId,

                                                                  @Parameter(description = "Sort by: recent, score", example = "recent") @RequestParam(defaultValue = "recent") String sortBy,

                                                                  @Parameter(description = "Page number", example = "0") @RequestParam(defaultValue = "0") int page,

                                                                  @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size,

                                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails != null ? userDetails.getUser().getId() : null;
        List<CommentResponse> comments = socialService.getAlertComments(alertId, sortBy, page, size, userId);
        return ResponseEntity.ok(comments);
    }

    @PutMapping("/comments/{commentId}")
    @Operation(summary = "Update comment", description = "Update a comment (owner only)", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CommentResponse> updateComment(@Parameter(description = "Comment ID", required = true) @PathVariable Long commentId, @Valid @RequestBody UpdateCommentRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {

        CommentResponse response = socialService.updateComment(commentId, request, userDetails.getUser().getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Delete comment", description = "Delete a comment (owner or admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteComment(@Parameter(description = "Comment ID", required = true) @PathVariable Long commentId, @AuthenticationPrincipal CustomUserDetails userDetails) {

        boolean isAdmin = userDetails.getUser().getRole() == Role.ADMIN;
        socialService.deleteComment(commentId, userDetails.getUser().getId(), isAdmin);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Comment deleted successfully");

        return ResponseEntity.ok(response);
    }

    // VOTING ENDPOINTS

    @PostMapping("/alerts/{alertId}/vote")
    @Operation(summary = "Vote on alert", description = "Upvote or downvote an alert", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> voteOnAlert(@Parameter(description = "Alert ID", required = true) @PathVariable Long alertId, @Valid @RequestBody VoteRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = socialService.voteOnAlert(alertId, request, userDetails.getUser().getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/comments/{commentId}/vote")
    @Operation(summary = "Vote on comment", description = "Upvote or downvote a comment", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> voteOnComment(@Parameter(description = "Comment ID", required = true) @PathVariable Long commentId, @Valid @RequestBody VoteRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> response = socialService.voteOnComment(commentId, request, userDetails.getUser().getId());
        return ResponseEntity.ok(response);
    }

    // PUBLIC VOTE COUNT ENDPOINTS

    @GetMapping("/alerts/{alertId}/votes")
    @Operation(summary = "Get alert vote counts (Public)", description = "Get vote statistics for a specific alert. This endpoint is public and does not require authentication.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Vote counts retrieved successfully"), @ApiResponse(responseCode = "404", description = "Alert not found")})
    public ResponseEntity<Map<String, Object>> getAlertVotes(@Parameter(description = "Alert ID", required = true) @PathVariable Long alertId, @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails != null ? userDetails.getUser().getId() : null;
        Map<String, Object> votes = socialService.getAlertVotes(alertId, userId);
        return ResponseEntity.ok(votes);
    }

    @GetMapping("/comments/{commentId}/votes")
    @Operation(summary = "Get comment vote counts (Public)", description = "Get vote statistics for a specific comment. This endpoint is public and does not require authentication.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Vote counts retrieved successfully"), @ApiResponse(responseCode = "404", description = "Comment not found")})
    public ResponseEntity<Map<String, Object>> getCommentVotes(@Parameter(description = "Comment ID", required = true) @PathVariable Long commentId, @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails != null ? userDetails.getUser().getId() : null;
        Map<String, Object> votes = socialService.getCommentVotes(commentId, userId);
        return ResponseEntity.ok(votes);
    }

    // USER ACTIVITY ENDPOINTS

    @GetMapping("/users/{userId}/comments")
    @Operation(summary = "Get user comments", description = "Get all comments by a specific user", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<CommentResponse>> getUserComments(@Parameter(description = "User ID", required = true) @PathVariable Long userId,

                                                                 @Parameter(description = "Page number", example = "0") @RequestParam(defaultValue = "0") int page,

                                                                 @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size,

                                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long requestingUserId = userDetails.getUser().getId();
        boolean isAdmin = userDetails.getUser().getRole() == Role.ADMIN;

        if (!isAdmin && !requestingUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<CommentResponse> comments = socialService.getUserComments(userId, page, size);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/comments/{commentId}/stats")
    @Operation(summary = "Get comment statistics", description = "Get vote statistics for a comment")
    public ResponseEntity<Map<String, Object>> getCommentStats(@Parameter(description = "Comment ID", required = true) @PathVariable Long commentId) {

        Map<String, Object> stats = socialService.getCommentStats(commentId);
        return ResponseEntity.ok(stats);
    }
}