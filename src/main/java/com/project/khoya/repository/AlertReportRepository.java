package com.project.khoya.repository;

import com.project.khoya.entity.AlertReport;
import com.project.khoya.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertReportRepository extends JpaRepository<AlertReport, Long> {


    boolean existsByAlertIdAndReportedById(Long alertId, Long userId);


    List<AlertReport> findByAlertIdOrderByCreatedAtDesc(Long alertId);


    Page<AlertReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);


    List<AlertReport> findByReportedByIdOrderByCreatedAtDesc(Long userId);


    long countByStatus(ReportStatus status);


    @Query("SELECT r FROM AlertReport r WHERE r.createdAt >= :since ORDER BY r.createdAt DESC")
    List<AlertReport> findRecentReports(@Param("since") LocalDateTime since);


    @Query("SELECT r FROM AlertReport r LEFT JOIN FETCH r.alert LEFT JOIN FETCH r.reportedBy " +
            "WHERE r.status = :status ORDER BY r.createdAt DESC")
    List<AlertReport> findReportsForAdminReview(@Param("status") ReportStatus status);
}
