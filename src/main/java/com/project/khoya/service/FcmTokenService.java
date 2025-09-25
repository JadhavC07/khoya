package com.project.khoya.service;

import com.project.khoya.dto.FcmTokenRequest;
import com.project.khoya.entity.FcmToken;
import com.project.khoya.entity.User;
import com.project.khoya.repository.FcmTokenRepository;
import com.project.khoya.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    public void saveOrUpdateToken(Long userId, FcmTokenRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<FcmToken> existingToken = fcmTokenRepository.findByTokenAndIsActiveTrue(request.getToken());

        if (existingToken.isPresent()) {
            // Update existing token
            FcmToken token = existingToken.get();
            token.setUser(user);
            token.setDeviceId(request.getDeviceId());
            token.setDeviceType(request.getDeviceType());
            fcmTokenRepository.save(token);
            log.info("Updated FCM token for user: {}", userId);
        } else {
            // Create new token
            FcmToken newToken = new FcmToken();
            newToken.setUser(user);
            newToken.setToken(request.getToken());
            newToken.setDeviceId(request.getDeviceId());
            newToken.setDeviceType(request.getDeviceType());
            newToken.setIsActive(true);
            fcmTokenRepository.save(newToken);
            log.info("Saved new FCM token for user: {}", userId);
        }
    }

    public void removeToken(Long userId, String token) {
        fcmTokenRepository.deleteByUserIdAndToken(userId, token);
        log.info("Removed FCM token for user: {}", userId);
    }
}