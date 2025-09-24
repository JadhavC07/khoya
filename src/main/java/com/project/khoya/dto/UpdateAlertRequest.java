package com.project.khoya.dto;

import com.project.khoya.entity.AlertStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request to update an existing alert")
public class UpdateAlertRequest {

    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    @Schema(description = "Alert title", example = "Missing Person - John Doe")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Schema(description = "Detailed description of the missing person")
    private String description;

    @Size(max = 200, message = "Location cannot exceed 200 characters")
    @Schema(description = "Last known location")
    private String location;

    @Schema(description = "URL to person's photo")
    private String imageUrl;

    @Schema(description = "Alert status")
    private AlertStatus status;
}
