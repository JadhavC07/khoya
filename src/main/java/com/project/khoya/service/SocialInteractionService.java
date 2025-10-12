package com.project.khoya.service;

import com.project.khoya.dto.CommentResponse;
import com.project.khoya.dto.CreateCommentRequest;
import com.project.khoya.dto.UpdateCommentRequest;
import com.project.khoya.dto.VoteRequest;
import com.project.khoya.entity.*;
import com.project.khoya.exception.AlertNotFoundException;
import com.project.khoya.exception.UnauthorizedOperationException;
import com.project.khoya.repository.CommentRepository;
import com.project.khoya.repository.MissingAlertRepository;
import com.project.khoya.repository.UserRepository;
import com.project.khoya.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SocialInteractionService {

    private final CommentRepository commentRepository;
    private final VoteRepository voteRepository;
    private final MissingAlertRepository alertRepository;
    private final UserRepository userRepository;

    // COMMENT OPERATIONS

    public CommentResponse createComment(Long alertId, CreateCommentRequest request, Long userId) {
        MissingAlert alert = alertRepository.findById(alertId).orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + alertId));

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setAlert(alert);
        comment.setAuthor(user);
        comment.setStatus(CommentStatus.ACTIVE);

        // Handle replies (nested comments like Instagram)
        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId()).orElseThrow(() -> new RuntimeException("Parent comment not found"));

            // Ensure parent belongs to same alert
            if (!parent.getAlert().getId().equals(alertId)) {
                throw new RuntimeException("Parent comment must belong to the same alert");
            }

            comment.setParent(parent);

            // Update parent's reply count
            parent.incrementReplyCount();
            commentRepository.save(parent);
        }

        Comment savedComment = commentRepository.save(comment);

        return mapToCommentResponse(savedComment, userId);
    }

    public CommentResponse updateComment(Long commentId, UpdateCommentRequest request, Long userId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("Comment not found"));

        // Check if user owns this comment
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedOperationException("You can only update your own comments");
        }

        comment.setContent(request.getContent());
        comment.setIsEdited(true);
        comment.setEditedAt(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);

        return mapToCommentResponse(savedComment, userId);
    }

    public void deleteComment(Long commentId, Long userId, boolean isAdmin) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("Comment not found"));

        // Only comment owner or admin can delete
        if (!isAdmin && !comment.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedOperationException("You can only delete your own comments");
        }

        // If comment has replies, mark as deleted instead of removing
        if (comment.getReplyCount() > 0) {
            comment.setContent("[Comment deleted]");
            comment.setStatus(CommentStatus.DELETED);
            commentRepository.save(comment);
        } else {
            // Update parent reply count if this is a reply
            if (comment.getParent() != null) {
                Comment parent = comment.getParent();
                parent.decrementReplyCount();
                commentRepository.save(parent);
            }
            commentRepository.delete(comment);
        }
    }

    public List<CommentResponse> getAlertComments(Long alertId, String sortBy, int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage;

        if ("score".equals(sortBy)) {
            commentPage = commentRepository.findTopScoredRootComments(alertId, CommentStatus.ACTIVE, pageable);
        } else {
            commentPage = commentRepository.findByAlertIdAndParentIsNullAndStatusOrderByCreatedAtDesc(alertId, CommentStatus.ACTIVE, pageable);
        }

        return commentPage.getContent().stream().map(comment -> mapToCommentResponseWithReplies(comment, userId)).collect(Collectors.toList());
    }

    // VOTING OPERATIONS

    public Map<String, Object> voteOnAlert(Long alertId, VoteRequest request, Long userId) {
        MissingAlert alert = alertRepository.findById(alertId).orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + alertId));

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent users from voting on their own alerts
        if (alert.getPostedBy().getId().equals(userId)) {
            throw new RuntimeException("You cannot vote on your own alert");
        }

        Optional<Vote> existingVote = voteRepository.findByUserIdAndAlertId(userId, alertId);

        Map<String, Object> response = new HashMap<>();

        if (existingVote.isPresent()) {
            Vote vote = existingVote.get();
            if (vote.getType() == request.getType()) {
                // Remove vote if same type
                voteRepository.delete(vote);
                response.put("action", "removed");
            } else {
                // Change vote type
                vote.setType(request.getType());
                voteRepository.save(vote);
                response.put("action", "changed");
            }
        } else {
            // Create new vote
            Vote vote = new Vote();
            vote.setUser(user);
            vote.setAlert(alert);
            vote.setType(request.getType());
            voteRepository.save(vote);
            response.put("action", "added");
        }

        // Get updated vote counts
        long upvotes = voteRepository.countByAlertIdAndType(alertId, VoteType.UP);
        long downvotes = voteRepository.countByAlertIdAndType(alertId, VoteType.DOWN);

        response.put("upvotes", upvotes);
        response.put("downvotes", downvotes);
        response.put("score", upvotes - downvotes);
        response.put("userVote", voteRepository.findByUserIdAndAlertId(userId, alertId).map(Vote::getType).orElse(null));
        return response;
    }

    public Map<String, Object> voteOnComment(Long commentId, VoteRequest request, Long userId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("Comment not found"));

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent users from voting on their own comments
        if (comment.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("You cannot vote on your own comment");
        }

        Optional<Vote> existingVote = voteRepository.findByUserIdAndCommentId(userId, commentId);

        Map<String, Object> response = new HashMap<>();

        if (existingVote.isPresent()) {
            Vote vote = existingVote.get();
            if (vote.getType() == request.getType()) {
                // Remove vote if same type
                voteRepository.delete(vote);
                if (request.getType() == VoteType.UP) {
                    comment.decrementUpvotes();
                } else {
                    comment.decrementDownvotes();
                }
                response.put("action", "removed");
            } else {
                // Change vote type
                if (vote.getType() == VoteType.UP) {
                    comment.decrementUpvotes();
                    comment.incrementDownvotes();
                } else {
                    comment.decrementDownvotes();
                    comment.incrementUpvotes();
                }
                vote.setType(request.getType());
                voteRepository.save(vote);
                response.put("action", "changed");
            }
        } else {
            // Create new vote
            Vote vote = new Vote();
            vote.setUser(user);
            vote.setComment(comment);
            vote.setType(request.getType());
            voteRepository.save(vote);

            if (request.getType() == VoteType.UP) {
                comment.incrementUpvotes();
            } else {
                comment.incrementDownvotes();
            }
            response.put("action", "added");
        }

        commentRepository.save(comment);

        response.put("upvotes", comment.getUpvotes());
        response.put("downvotes", comment.getDownvotes());
        response.put("score", comment.getScore());
        response.put("userVote", voteRepository.findByUserIdAndCommentId(userId, commentId).map(Vote::getType).orElse(null));
        return response;
    }

    // PUBLIC VOTE COUNT METHODS
    public Map<String, Object> getAlertVotes(Long alertId, Long userId) {
        // Verify alert exists
        alertRepository.findById(alertId).orElseThrow(() -> new AlertNotFoundException("Alert not found with id: " + alertId));

        Map<String, Object> response = new HashMap<>();

        // Get vote counts
        long upvotes = voteRepository.countByAlertIdAndType(alertId, VoteType.UP);
        long downvotes = voteRepository.countByAlertIdAndType(alertId, VoteType.DOWN);

        response.put("alertId", alertId);
        response.put("upvotes", upvotes);
        response.put("downvotes", downvotes);
        response.put("score", upvotes - downvotes);

        // Include user's vote if authenticated
        if (userId != null) {
            VoteType userVote = voteRepository.findByUserIdAndAlertId(userId, alertId).map(Vote::getType).orElse(null);
            response.put("userVote", userVote);
        } else {
            response.put("userVote", null);
        }
        return response;
    }

    /**
     * Get vote counts for a specific comment (PUBLIC - no authentication required)
     * Returns upvotes, downvotes, score, and current user's vote if authenticated
     */
    public Map<String, Object> getCommentVotes(Long commentId, Long userId) {
        // Verify comment exists
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        Map<String, Object> response = new HashMap<>();

        response.put("commentId", commentId);
        response.put("alertId", comment.getAlert().getId());
        response.put("upvotes", comment.getUpvotes());
        response.put("downvotes", comment.getDownvotes());
        response.put("score", comment.getScore());

        // Include user's vote if authenticated
        if (userId != null) {
            VoteType userVote = voteRepository.findByUserIdAndCommentId(userId, commentId).map(Vote::getType).orElse(null);
            response.put("userVote", userVote);
        } else {
            response.put("userVote", null);
        }
        return response;
    }

    // HELPER METHODS

    private CommentResponse mapToCommentResponse(Comment comment, Long currentUserId) {
        VoteType userVote = currentUserId != null ? voteRepository.findByUserIdAndCommentId(currentUserId, comment.getId()).map(Vote::getType).orElse(null) : null;

        boolean canEdit = comment.getAuthor().getId().equals(currentUserId);
        boolean canDelete = canEdit; // For now, same permissions

        return CommentResponse.builder().id(comment.getId()).content(comment.getContent()).alertId(comment.getAlert().getId()).parentId(comment.getParent() != null ? comment.getParent().getId() : null).upvotes(comment.getUpvotes()).downvotes(comment.getDownvotes()).score(comment.getScore()).replyCount(comment.getReplyCount()).isEdited(comment.getIsEdited()).status(comment.getStatus()).createdAt(comment.getCreatedAt()).updatedAt(comment.getUpdatedAt()).editedAt(comment.getEditedAt()).author(CommentResponse.UserInfo.builder().id(comment.getAuthor().getId()).name(comment.getAuthor().getName()).email(comment.getAuthor().getEmail()).build()).userVote(userVote).canEdit(canEdit).canDelete(canDelete).build();
    }

    private CommentResponse mapToCommentResponseWithReplies(Comment comment, Long currentUserId) {
        CommentResponse response = mapToCommentResponse(comment, currentUserId);

        // Add replies if this is a root comment (supports nested comments like Instagram)
        if (comment.isRootComment()) {
            List<Comment> replies = commentRepository.findByParentIdAndStatusOrderByCreatedAtAsc(comment.getId(), CommentStatus.ACTIVE);

            List<CommentResponse> replyResponses = replies.stream().map(reply -> mapToCommentResponse(reply, currentUserId)).collect(Collectors.toList());

            response.setReplies(replyResponses);
        }

        return response;
    }

    public Map<String, Object> getCommentStats(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("Comment not found"));

        Map<String, Object> stats = new HashMap<>();
        stats.put("upvotes", comment.getUpvotes());
        stats.put("downvotes", comment.getDownvotes());
        stats.put("score", comment.getScore());
        stats.put("replyCount", comment.getReplyCount());

        return stats;
    }

    public List<CommentResponse> getUserComments(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(userId, CommentStatus.ACTIVE, pageable);

        return comments.getContent().stream().map(comment -> mapToCommentResponse(comment, userId)).collect(Collectors.toList());
    }
}