package com.project.khoya.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.khoya.entity.AlertStatus;
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
@Schema(description = "Alert response")
public class AlertResponse {

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        @Schema(description = "User ID", example = "1")
        private Long id;

        @Schema(description = "User name", example = "John Doe")
        private String name;

        @Schema(description = "User email", example = "john@example.com")
        private String email;
    }
}


