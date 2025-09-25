package com.project.khoya.repository;

import com.project.khoya.entity.Vote;
import com.project.khoya.entity.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    // Find user's vote on an alert
    Optional<Vote> findByUserIdAndAlertId(Long userId, Long alertId);

    // Find user's vote on a comment
    Optional<Vote> findByUserIdAndCommentId(Long userId, Long commentId);

    // Check if user has voted on alert
    boolean existsByUserIdAndAlertId(Long userId, Long alertId);

    // Check if user has voted on comment
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    // Count votes by type for alert
    long countByAlertIdAndType(Long alertId, VoteType type);

    // Count votes by type for comment
    long countByCommentIdAndType(Long commentId, VoteType type);

    // Get vote counts for multiple alerts (for performance)
    @Query("SELECT v.alert.id, v.type, COUNT(v) FROM Vote v WHERE v.alert.id IN :alertIds GROUP BY v.alert.id, v.type")
    Object[][] getVoteCountsForAlerts(@Param("alertIds") java.util.List<Long> alertIds);

    // Get vote counts for multiple comments (for performance)
    @Query("SELECT v.comment.id, v.type, COUNT(v) FROM Vote v WHERE v.comment.id IN :commentIds GROUP BY v.comment.id, v.type")
    Object[][] getVoteCountsForComments(@Param("commentIds") java.util.List<Long> commentIds);

    // Get user votes for multiple alerts
    @Query("SELECT v.alert.id, v.type FROM Vote v WHERE v.user.id = :userId AND v.alert.id IN :alertIds")
    Object[][] getUserVotesForAlerts(@Param("userId") Long userId, @Param("alertIds") java.util.List<Long> alertIds);

    // Get user votes for multiple comments
    @Query("SELECT v.comment.id, v.type FROM Vote v WHERE v.user.id = :userId AND v.comment.id IN :commentIds")
    Object[][] getUserVotesForComments(@Param("userId") Long userId, @Param("commentIds") java.util.List<Long> commentIds);
}
