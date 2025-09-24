package com.project.khoya.dto;

import com.project.khoya.entity.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request to report an alert")
public class ReportAlertRequest {

    @NotNull(message = "Report reason is required")
    @Schema(description = "Reason for reporting the alert")
    private ReportReason reason;

    @Size(max = 500, message = "Additional details cannot exceed 500 characters")
    @Schema(description = "Additional details about the report",
            example = "This alert appears to be fake based on inconsistent information")
    private String additionalDetails;
}
