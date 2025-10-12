package com.project.khoya.controller;

import com.project.khoya.entity.CommentStatus;
import com.project.khoya.repository.CommentRepository;
import com.project.khoya.repository.VoteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/social")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Social Moderation", description = "Admin operations for social interaction moderation")
public class AdminSocialModerationController {

    private final CommentRepository commentRepository;
    private final VoteRepository voteRepository;

    @GetMapping("/stats")
    @Operation(summary = "Get social interaction statistics", description = "Get comprehensive statistics about comments and votes", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, Object>> getSocialStats() {
        Map<String, Object> stats = new HashMap<>();

        // Comment statistics
        stats.put("totalComments", commentRepository.count());
        stats.put("activeComments", commentRepository.countByAlertIdAndStatus(null, CommentStatus.ACTIVE));
        stats.put("flaggedComments", commentRepository.findCommentsNeedingModeration(5).size());
        stats.put("deletedComments", commentRepository.countByAlertIdAndStatus(null, CommentStatus.DELETED));

        // Vote statistics
        stats.put("totalVotes", voteRepository.count());

        // Recent activity (last 24 hours)
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        stats.put("recentComments", commentRepository.findRecentComments(yesterday, CommentStatus.ACTIVE).size());

        // Top performing comments
        stats.put("highScoredComments", commentRepository.findHighScoredComments(10, CommentStatus.ACTIVE).size());

        return ResponseEntity.ok(stats);
    }

    @PutMapping("/comments/{commentId}/status")
    @Operation(summary = "Update comment status", description = "Update the status of a comment (hide, flag, etc.)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> updateCommentStatus(@Parameter(description = "Comment ID", required = true) @PathVariable Long commentId,

                                                                   @Parameter(description = "New comment status", required = true) @RequestParam CommentStatus status,

                                                                   @Parameter(description = "Admin notes") @RequestParam(required = false) String notes) {

        var comment = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("Comment not found"));

        comment.setStatus(status);
        commentRepository.save(comment);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Comment status updated to " + status);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/comments/flagged")
    @Operation(summary = "Get flagged comments", description = "Get comments that need moderation attention", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, Object>> getFlaggedComments(@Parameter(description = "Minimum downvote threshold") @RequestParam(defaultValue = "5") int downvoteThreshold) {

        var flaggedComments = commentRepository.findCommentsNeedingModeration(downvoteThreshold);

        var commentData = flaggedComments.stream().map(comment -> Map.of("id", comment.getId(), "content", comment.getContent(), "alertId", comment.getAlert().getId(), "alertTitle", comment.getAlert().getTitle(), "score", comment.getScore(), "upvotes", comment.getUpvotes(), "downvotes", comment.getDownvotes(), "status", comment.getStatus(), "createdAt", comment.getCreatedAt(), "author", Map.of("id", comment.getAuthor().getId(), "name", comment.getAuthor().getName(), "email", comment.getAuthor().getEmail()))).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("comments", commentData);
        response.put("count", commentData.size());
        response.put("threshold", downvoteThreshold);

        return ResponseEntity.ok(response);
    }
}
