package com.project.khoya.repository;

import com.project.khoya.entity.Comment;
import com.project.khoya.entity.CommentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Find root comments for an alert (no parent)
    Page<Comment> findByAlertIdAndParentIsNullAndStatusOrderByCreatedAtDesc(
            Long alertId, CommentStatus status, Pageable pageable);

    // Find all comments for an alert (including replies)
    List<Comment> findByAlertIdAndStatusOrderByCreatedAtAsc(Long alertId, CommentStatus status);

    // Find replies to a specific comment
    List<Comment> findByParentIdAndStatusOrderByCreatedAtAsc(Long parentId, CommentStatus status);

    // Count comments for an alert
    long countByAlertIdAndStatus(Long alertId, CommentStatus status);

    // Find comments by user
    Page<Comment> findByAuthorIdAndStatusOrderByCreatedAtDesc(Long userId, CommentStatus status, Pageable pageable);

    // Find top-scored comments for an alert
    @Query("SELECT c FROM Comment c WHERE c.alert.id = :alertId AND c.parent IS NULL AND c.status = :status " +
            "ORDER BY c.score DESC, c.createdAt DESC")
    Page<Comment> findTopScoredRootComments(@Param("alertId") Long alertId,
                                            @Param("status") CommentStatus status,
                                            Pageable pageable);

    // Find recent comments
    @Query("SELECT c FROM Comment c WHERE c.createdAt >= :since AND c.status = :status ORDER BY c.createdAt DESC")
    List<Comment> findRecentComments(@Param("since") LocalDateTime since, @Param("status") CommentStatus status);

    // Count replies for a comment
    long countByParentIdAndStatus(Long parentId, CommentStatus status);

    // Find comments with high scores (for admin review)
    @Query("SELECT c FROM Comment c WHERE c.score >= :threshold AND c.status = :status ORDER BY c.score DESC")
    List<Comment> findHighScoredComments(@Param("threshold") int threshold, @Param("status") CommentStatus status);

    // Find comments that need moderation (flagged or many downvotes)
    @Query("SELECT c FROM Comment c WHERE (c.status = 'FLAGGED' OR c.downvotes >= :downvoteThreshold) " +
            "ORDER BY c.downvotes DESC, c.createdAt DESC")
    List<Comment> findCommentsNeedingModeration(@Param("downvoteThreshold") int downvoteThreshold);

    // Comment Search
    @Query("SELECT c FROM Comment c WHERE c.content LIKE %:keyword% " +
            "AND c.status = 'ACTIVE' ORDER BY c.score DESC")
    Page<Comment> searchComments(@Param("keyword") String keyword, Pageable pageable);

    List<Comment> findByAlertIdAndParentIsNull(Long alertId);

}
