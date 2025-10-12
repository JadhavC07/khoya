package com.project.khoya.service;

import com.project.khoya.config.CustomUserDetails;
import com.project.khoya.config.JwtUtil;
import com.project.khoya.dto.AuthResponse;
import com.project.khoya.dto.LoginRequest;
import com.project.khoya.dto.RegisterRequest;
import com.project.khoya.dto.auth.RefreshTokenRequest;
import com.project.khoya.entity.RefreshToken;
import com.project.khoya.entity.Role;
import com.project.khoya.entity.User;
import com.project.khoya.exception.TokenRefreshException;
import com.project.khoya.exception.UserAlreadyExistsException;
import com.project.khoya.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }

        // Create new user
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setActive(true);

        User savedUser = userRepository.save(user);
        log.info("New user registered with email: {}", savedUser.getEmail());

        // Generate tokens
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String accessToken = jwtUtil.generateAccessToken(userDetails, savedUser.getId(), savedUser.getRole().name());

        // Create refresh token
        String deviceInfo = refreshTokenService.getDeviceInfo(httpRequest);
        String ipAddress = refreshTokenService.getClientIP(httpRequest);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser.getId(), deviceInfo, ipAddress);

        return AuthResponse.builder().status("success").message("User registered successfully").accessToken(accessToken).refreshToken(refreshToken.getToken()).tokenType("Bearer").userId(savedUser.getId()).name(savedUser.getName()).email(savedUser.getEmail()).role(savedUser.getRole()).accessTokenExpiresAt(System.currentTimeMillis() + jwtUtil.getAccessTokenExpiration()).refreshTokenExpiresAt(System.currentTimeMillis() + jwtUtil.getRefreshTokenExpiration()).build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            // Generate access token
            String accessToken = jwtUtil.generateAccessToken(userDetails, user.getId(), user.getRole().name());

            // Create refresh token
            String deviceInfo = refreshTokenService.getDeviceInfo(httpRequest);
            String ipAddress = refreshTokenService.getClientIP(httpRequest);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId(), deviceInfo, ipAddress);

            log.info("User logged in: {} from IP: {}", user.getEmail(), ipAddress);

            return AuthResponse.builder().status("success").message("Login successful").accessToken(accessToken).refreshToken(refreshToken.getToken()).tokenType("Bearer").userId(user.getId()).name(user.getName()).email(user.getEmail()).role(user.getRole()).accessTokenExpiresAt(System.currentTimeMillis() + jwtUtil.getAccessTokenExpiration()).refreshTokenExpiresAt(System.currentTimeMillis() + jwtUtil.getRefreshTokenExpiration()).build();

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken);
        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        String newAccessToken = jwtUtil.generateAccessToken(userDetails, user.getId(), user.getRole().name());

        log.info("Access token refreshed for user: {}", user.getEmail());

        return AuthResponse.builder().status("success").message("Token refreshed successfully").accessToken(newAccessToken).refreshToken(requestRefreshToken).tokenType("Bearer").userId(user.getId()).name(user.getName()).email(user.getEmail()).role(user.getRole()).accessTokenExpiresAt(System.currentTimeMillis() + jwtUtil.getAccessTokenExpiration()).refreshTokenExpiresAt(System.currentTimeMillis() + jwtUtil.getRefreshTokenExpiration()).build();
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        // Blacklist the access token
        if (accessToken != null && !accessToken.isEmpty()) {
            tokenBlacklistService.blacklistToken(accessToken, "User logout");
        }

        // Revoke the refresh token
        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                refreshTokenService.revokeRefreshToken(refreshToken);
            } catch (TokenRefreshException e) {
                log.warn("Refresh token not found during logout: {}", e.getMessage());
            }
        }

        log.info("User logged out successfully");
    }

    @Transactional
    public void logoutAllDevices(Long userId, String currentAccessToken) {
        // Blacklist current access token
        if (currentAccessToken != null && !currentAccessToken.isEmpty()) {
            tokenBlacklistService.blacklistToken(currentAccessToken, "Logout all devices");
        }

        // Revoke all refresh tokens for this user
        refreshTokenService.revokeAllUserTokens(userId);

        log.info("User {} logged out from all devices", userId);
    }
}