package com.project.khoya.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request to create a new missing person alert")
public class CreateAlertRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    @Schema(description = "Alert title", example = "Missing Person - John Doe")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Schema(description = "Detailed description of the missing person", example = "Last seen wearing blue jeans and red shirt near Central Park")
    private String description;

    @Size(max = 200, message = "Location cannot exceed 200 characters")
    @Schema(description = "Last known location", example = "Central Park, New York")
    private String location;

    @Schema(description = "URL to person's photo", example = "https://example.com/photo.jpg")
    private String imageUrl;
}
