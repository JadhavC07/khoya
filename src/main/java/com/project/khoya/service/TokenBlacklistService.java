package com.project.khoya.service;

import com.project.khoya.config.JwtUtil;
import com.project.khoya.entity.TokenBlacklist;
import com.project.khoya.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public void blacklistToken(String token, String reason) {
        if (tokenBlacklistRepository.existsByToken(token)) {
            log.warn("Token already blacklisted: {}", token.substring(0, 20) + "...");
            return;
        }

        Date expiration = jwtUtil.extractExpiration(token);
        LocalDateTime expiryDate = expiration.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        TokenBlacklist blacklistedToken = TokenBlacklist.builder()
                .token(token)
                .expiryDate(expiryDate)
                .reason(reason)
                .build();

        tokenBlacklistRepository.save(blacklistedToken);
        log.info("Token blacklisted: {}", token.substring(0, 20) + "...");
    }

    public boolean isBlacklisted(String token) {
        return tokenBlacklistRepository.existsByToken(token);
    }

    // Clean up expired blacklisted tokens every day at 3 AM
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired blacklisted tokens");
        tokenBlacklistRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Completed cleanup of expired blacklisted tokens");
    }
}