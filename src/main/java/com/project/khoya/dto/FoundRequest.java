package com.project.khoya.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request to mark alert as found")
public class FoundRequest {

    @Size(max = 500, message = "Found details cannot exceed 500 characters")
    @Schema(description = "Details about how/where the person was found", example = "Found safe at friend's house")
    private String foundDetails;
}