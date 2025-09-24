package com.project.khoya.repository;

import com.project.khoya.entity.AlertStatus;
import com.project.khoya.entity.MissingAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MissingAlertRepository extends JpaRepository<MissingAlert, Long> {

    // Find alerts by location (case-insensitive search)
    Page<MissingAlert> findByLocationContainingIgnoreCase(String location, Pageable pageable);

    // Find alerts by status
    Page<MissingAlert> findByStatus(AlertStatus status, Pageable pageable);

    // Find alerts by location and status
    Page<MissingAlert> findByLocationContainingIgnoreCaseAndStatus(String location, AlertStatus status, Pageable pageable);

    // Find alerts posted by specific user
    List<MissingAlert> findByPostedByIdOrderByCreatedAtDesc(Long userId);

    // Find active alerts (not found or closed)
    @Query("SELECT a FROM MissingAlert a WHERE a.status IN ('ACTIVE', 'UNDER_REVIEW') ORDER BY a.createdAt DESC")
    List<MissingAlert> findActiveAlerts();

    // Find recent alerts (last 30 days)
    @Query("SELECT a FROM MissingAlert a WHERE a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<MissingAlert> findRecentAlerts(@Param("since") LocalDateTime since);

    // Count alerts by status
    long countByStatus(AlertStatus status);

    // Count alerts by user
    long countByPostedById(Long userId);

    // Find alerts with high report count (for admin review)
    @Query("SELECT a FROM MissingAlert a WHERE a.reportCount >= :threshold ORDER BY a.reportCount DESC")
    List<MissingAlert> findAlertsWithHighReportCount(@Param("threshold") int threshold);
}

