package com.project.khoya.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Enhanced paginated alert list response with social data")
public class EnhancedAlertListResponse {

    @Schema(description = "List of enhanced alerts with social data")
    private List<EnhancedAlertResponse> alerts;

    @Schema(description = "Current page number")
    private int page;

    @Schema(description = "Number of items per page")
    private int size;

    @Schema(description = "Total number of alerts")
    private long totalElements;

    @Schema(description = "Total number of pages")
    private int totalPages;

    @Schema(description = "Whether this is the first page")
    private boolean isFirst;

    @Schema(description = "Whether this is the last page")
    private boolean isLast;

    @Schema(description = "Current sort order")
    private String sortBy;

    @Schema(description = "Overall engagement statistics")
    private EngagementStats stats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngagementStats {

        @Schema(description = "Total votes across all alerts in this page")
        private Long totalVotes;

        @Schema(description = "Total comments across all alerts in this page")
        private Long totalComments;

        @Schema(description = "Average engagement score")
        private Double averageEngagement;

        @Schema(description = "Number of trending alerts in this page")
        private Integer trendingCount;
    }
}