package com.project.khoya.dto;

import com.project.khoya.entity.VoteType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to vote on an alert or comment")
public class VoteRequest {

    @NotNull(message = "Vote type is required")
    @Schema(description = "Vote type - UP or DOWN", example = "UP")
    private VoteType type;
}
