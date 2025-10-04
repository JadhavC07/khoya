package com.project.khoya.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Logout request")
public class LogoutRequest {

    @Schema(description = "Refresh token to revoke", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String refreshToken;
}