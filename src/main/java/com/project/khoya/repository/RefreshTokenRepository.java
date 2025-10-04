package com.project.khoya.repository;

import com.project.khoya.entity.RefreshToken;
import com.project.khoya.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUser(User user);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user = :user")
    void deleteByUser(User user);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now OR rt.revoked = true")
    void deleteExpiredAndRevoked(LocalDateTime now);

    @Query("SELECT COUNT(rt) > 0 FROM RefreshToken rt WHERE rt.token = :token AND rt.revoked = false AND rt.expiryDate > :now")
    boolean isValidToken(String token, LocalDateTime now);
}