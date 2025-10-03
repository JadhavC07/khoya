package com.project.khoya.service;

import com.project.khoya.config.CustomUserDetails;
import com.project.khoya.config.JwtUtil;
import com.project.khoya.dto.AuthResponse;
import com.project.khoya.dto.LoginRequest;
import com.project.khoya.dto.RegisterRequest;
import com.project.khoya.entity.Role;
import com.project.khoya.entity.User;
import com.project.khoya.exception.UserAlreadyExistsException;
import com.project.khoya.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {

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

        // Generate token
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String token = jwtUtil.generateToken(userDetails, savedUser.getId(), savedUser.getRole().name());

        return AuthResponse.builder()
                .status("success")
                .message("User registered successfully")
                .token(token)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .expiresAt(System.currentTimeMillis() + jwtUtil.getExpirationTime())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            // Generate token
            String token = jwtUtil.generateToken(userDetails, user.getId(), user.getRole().name());

            log.info("User logged in: {}", user.getEmail());

            return AuthResponse.builder()
                    .status("success")
                    .message("Login successful")
                    .token(token)
                    .tokenType("Bearer")
                    .userId(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .expiresAt(System.currentTimeMillis() + jwtUtil.getExpirationTime())
                    .build();

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }
}
