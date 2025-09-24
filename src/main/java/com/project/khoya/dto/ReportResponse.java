package com.project.khoya.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.khoya.entity.ReportReason;
import com.project.khoya.entity.ReportStatus;
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
@Schema(description = "Report response")
public class ReportResponse {

    @Schema(description = "Report ID")
    private Long id;

    @Schema(description = "Alert ID that was reported")
    private Long alertId;

    @Schema(description = "Alert title")
    private String alertTitle;

    @Schema(description = "Report reason")
    private ReportReason reason;

    @Schema(description = "Additional details provided")
    private String additionalDetails;

    @Schema(description = "Report status")
    private ReportStatus status;

    @Schema(description = "When the report was created")
    private LocalDateTime createdAt;

    @Schema(description = "When the report was reviewed")
    private LocalDateTime reviewedAt;

    @Schema(description = "Information about who reported")
    private UserInfo reportedBy;

    @Schema(description = "Information about who reviewed")
    private UserInfo reviewedBy;

    @Schema(description = "Admin notes on the report")
    private String adminNotes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String name;
        private String email;
    }
}
