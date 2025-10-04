package com.project.khoya.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.khoya.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
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

    @Schema(description = "JWT access token (short-lived, 15 minutes)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Refresh token (long-lived, 30 days)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String refreshToken;

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

    @Schema(description = "Access token expiration time in milliseconds", example = "1640995200000")
    private Long accessTokenExpiresAt;

    @Schema(description = "Refresh token expiration time in milliseconds", example = "1643673600000")
    private Long refreshTokenExpiresAt;
}