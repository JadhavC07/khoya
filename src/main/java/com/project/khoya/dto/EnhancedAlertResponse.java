package com.project.khoya.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.khoya.entity.AlertStatus;
import com.project.khoya.entity.VoteType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Enhanced alert response with social interaction data")
public class EnhancedAlertResponse {

    @Schema(description = "Alert ID", example = "1")
    private Long id;

    @Schema(description = "Alert title", example = "Missing Person - John Doe")
    private String title;

    @Schema(description = "Alert description")
    private String description;

    @Schema(description = "Last known location")
    private String location;

    @Schema(description = "Photo URL")
    private String imageUrl;

    @Schema(description = "Alert status")
    private AlertStatus status;

    @Schema(description = "Number of reports received")
    private int reportCount;

    @Schema(description = "When the alert was created")
    private LocalDateTime createdAt;

    @Schema(description = "When the alert was last updated")
    private LocalDateTime updatedAt;

    @Schema(description = "When the person was found (if applicable)")
    private LocalDateTime foundAt;

    @Schema(description = "Information about who posted the alert")
    private UserInfo postedBy;

    // Social interaction data
    @Schema(description = "Number of upvotes on this alert", example = "15")
    private Integer upvotes;

    @Schema(description = "Number of downvotes on this alert", example = "2")
    private Integer downvotes;

    @Schema(description = "Alert score (upvotes - downvotes)", example = "13")
    private Integer score;

    @Schema(description = "Total number of comments", example = "8")
    private Integer commentCount;

    @Schema(description = "Current user's vote on this alert")
    private VoteType userVote;

    // Permission flags
    @Schema(description = "Whether current user can edit this alert")
    private Boolean canEdit;

    @Schema(description = "Whether current user can delete this alert")
    private Boolean canDelete;

    @Schema(description = "Whether current user can report this alert")
    private Boolean canReport;

    // Engagement metrics
    @Schema(description = "Total engagement score based on comments and votes")
    private Double engagementScore;

    @Schema(description = "Whether this alert is trending")
    private Boolean trending;

    @Schema(description = "Alert visibility status")
    private AlertVisibility visibility;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String name;
        private String email;

        @Schema(description = "User's reputation score")
        private Integer reputation;

        @Schema(description = "Number of alerts posted by user")
        private Integer alertCount;
    }

    public enum AlertVisibility {
        PUBLIC, FLAGGED, HIDDEN, ARCHIVED
    }
}