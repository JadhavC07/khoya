package com.project.khoya.dto;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.khoya.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Authentication response")
public class AuthResponse {

    @Schema(description = "Response status", example = "success")
    private String status;

    @Schema(description = "Response message", example = "Login successful")
    private String message;

    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Token type", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "User ID", example = "1")
    private Long userId;

    @Schema(description = "User name", example = "John Doe")
    private String name;

    @Schema(description = "User email", example = "john.doe@example.com")
    private String email;

    @Schema(description = "User role", example = "USER")
    private Role role;

    @Schema(description = "Token expiration time in milliseconds", example = "1640995200000")
    private Long expiresAt;
}
