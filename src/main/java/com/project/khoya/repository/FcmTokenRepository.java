package com.project.khoya.repository;

import com.project.khoya.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    List<FcmToken> findByUserIdAndIsActiveTrue(Long userId);

    Optional<FcmToken> findByTokenAndIsActiveTrue(String token);

    @Query("SELECT f FROM FcmToken f WHERE f.user.id IN :userIds AND f.isActive = true")
    List<FcmToken> findActiveTokensByUserIds(List<Long> userIds);

    @Query("SELECT f FROM FcmToken f WHERE f.isActive = true")
    List<FcmToken> findAllActiveTokens();

    void deleteByUserIdAndToken(Long userId, String token);
}