package com.project.khoya.controller;

import com.project.khoya.config.CustomUserDetails;
import com.project.khoya.config.JwtUtil;
import com.project.khoya.dto.AuthResponse;
import com.project.khoya.dto.LoginRequest;
import com.project.khoya.dto.RegisterRequest;
import com.project.khoya.dto.auth.LogoutRequest;
import com.project.khoya.dto.auth.RefreshTokenRequest;
import com.project.khoya.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Create a new user account with email and password. Returns both access and refresh tokens."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input or user already exists"
            )
    })
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticate user with email and password. Returns both access token (15 min) and refresh token (30 days)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials"
            )
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Get a new access token using refresh token. Use this when access token expires."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Invalid or expired refresh token"
            )
    })
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "User logout",
            description = "Logout current user. Blacklists the access token and revokes the refresh token."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Logout successful"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<AuthResponse> logout(
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpRequest) {

        // Extract access token from header
        String accessToken = parseJwt(httpRequest);

        // Get refresh token from request body
        String refreshToken = request != null ? request.getRefreshToken() : null;

        authService.logout(accessToken, refreshToken);

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .status("success")
                        .message("Logout successful")
                        .build()
        );
    }

    @PostMapping("/logout-all")
    @Operation(
            summary = "Logout from all devices",
            description = "Logout user from all devices. Revokes all refresh tokens and blacklists current access token."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Logged out from all devices successfully"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<AuthResponse> logoutAllDevices(
            Authentication authentication,
            HttpServletRequest httpRequest) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        String accessToken = parseJwt(httpRequest);

        authService.logoutAllDevices(userId, accessToken);

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .status("success")
                        .message("Logged out from all devices successfully")
                        .build()
        );
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}