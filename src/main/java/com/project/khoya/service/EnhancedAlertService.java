package com.project.khoya.service;

import com.project.khoya.dto.*;
import com.project.khoya.entity.*;
import com.project.khoya.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedAlertService {

    private final MissingAlertRepository alertRepository;
    private final CommentRepository commentRepository;
    private final VoteRepository voteRepository;
    private final AlertReportRepository reportRepository;
    private final UserRepository userRepository;

    public EnhancedAlertListResponse getAlertsWithSocialData(int page, int size, String location,
                                                             AlertStatus status, String sortBy, Long currentUserId) {

        Sort sort = determineSorting(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MissingAlert> alertPage = getFilteredAlerts(location, status, pageable);
        List<Long> alertIds = alertPage.getContent().stream().map(MissingAlert::getId).toList();

        // Batch load vote data for performance
        Map<Long, VoteData> voteDataMap = getVoteDataForAlerts(alertIds, currentUserId);
        Map<Long, Long> commentCountMap = getCommentCountsForAlerts(alertIds);

        List<EnhancedAlertResponse> alertResponses = alertPage.getContent().stream()
                .map(alert -> mapToEnhancedAlertResponse(alert, currentUserId, voteDataMap, commentCountMap))
                .collect(Collectors.toList());

        return EnhancedAlertListResponse.builder()
                .alerts(alertResponses)
                .page(alertPage.getNumber())
                .size(alertPage.getSize())
                .totalElements(alertPage.getTotalElements())
                .totalPages(alertPage.getTotalPages())
                .isFirst(alertPage.isFirst())
                .isLast(alertPage.isLast())
                .sortBy(sortBy)
                .build();
    }

    public EnhancedAlertResponse getAlertWithSocialData(Long alertId, Long currentUserId) {
        MissingAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        // Get individual vote data
        VoteData voteData = getVoteDataForAlert(alertId, currentUserId);
        long commentCount = commentRepository.countByAlertIdAndStatus(alertId, CommentStatus.ACTIVE);

        return mapToEnhancedAlertResponse(alert, currentUserId, voteData, commentCount);
    }

    public List<EnhancedAlertResponse> getTrendingAlerts(int limit, Long currentUserId) {
        // Simple trending algorithm based on recent engagement
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<MissingAlert> alerts = alertRepository.findRecentAlerts(since)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());

        return alerts.stream()
                .map(alert -> {
                    VoteData voteData = getVoteDataForAlert(alert.getId(), currentUserId);
                    long commentCount = commentRepository.countByAlertIdAndStatus(alert.getId(), CommentStatus.ACTIVE);
                    EnhancedAlertResponse response = mapToEnhancedAlertResponse(alert, currentUserId, voteData, commentCount);
                    response.setTrending(true);
                    return response;
                })
                .collect(Collectors.toList());
    }

    public List<EnhancedAlertResponse> getTopRatedAlerts(int limit, Long currentUserId) {
        // Get alerts with highest scores
        List<MissingAlert> alerts = alertRepository.findAll()
                .stream()
                .sorted((a1, a2) -> Integer.compare(a2.getScore(), a1.getScore()))
                .limit(limit)
                .collect(Collectors.toList());

        return alerts.stream()
                .map(alert -> {
                    VoteData voteData = getVoteDataForAlert(alert.getId(), currentUserId);
                    long commentCount = commentRepository.countByAlertIdAndStatus(alert.getId(), CommentStatus.ACTIVE);
                    return mapToEnhancedAlertResponse(alert, currentUserId, voteData, commentCount);
                })
                .collect(Collectors.toList());
    }

    public List<EnhancedAlertResponse> getMostCommentedAlerts(int limit, Long currentUserId) {
        // Get alerts with most comments
        List<MissingAlert> alerts = alertRepository.findAll()
                .stream()
                .sorted((a1, a2) -> Integer.compare(a2.getCommentCount(), a1.getCommentCount()))
                .limit(limit)
                .collect(Collectors.toList());

        return alerts.stream()
                .map(alert -> {
                    VoteData voteData = getVoteDataForAlert(alert.getId(), currentUserId);
                    long commentCount = alert.getCommentCount();
                    return mapToEnhancedAlertResponse(alert, currentUserId, voteData, commentCount);
                })
                .collect(Collectors.toList());
    }

    private Sort determineSorting(String sortBy) {
        return switch (sortBy != null ? sortBy.toLowerCase() : "recent") {
            case "score" -> Sort.by("score").descending().and(Sort.by("createdAt").descending());
            case "comments" -> Sort.by("commentCount").descending().and(Sort.by("createdAt").descending());
            case "engagement" -> Sort.by("score").descending().and(Sort.by("commentCount").descending());
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.by("createdAt").descending(); // recent
        };
    }

    private Page<MissingAlert> getFilteredAlerts(String location, AlertStatus status, Pageable pageable) {
        if (location != null && !location.trim().isEmpty()) {
            if (status != null) {
                return alertRepository.findByLocationContainingIgnoreCaseAndStatus(
                        location.trim(), status, pageable);
            } else {
                return alertRepository.findByLocationContainingIgnoreCase(
                        location.trim(), pageable);
            }
        } else {
            if (status != null) {
                return alertRepository.findByStatus(status, pageable);
            } else {
                return alertRepository.findAll(pageable);
            }
        }
    }

    private Map<Long, VoteData> getVoteDataForAlerts(List<Long> alertIds, Long currentUserId) {
        return alertIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        alertId -> getVoteDataForAlert(alertId, currentUserId)
                ));
    }

    private VoteData getVoteDataForAlert(Long alertId, Long currentUserId) {
        long upvotes = voteRepository.countByAlertIdAndType(alertId, VoteType.UP);
        long downvotes = voteRepository.countByAlertIdAndType(alertId, VoteType.DOWN);

        VoteType userVote = null;
        if (currentUserId != null) {
            userVote = voteRepository.findByUserIdAndAlertId(currentUserId, alertId)
                    .map(Vote::getType)
                    .orElse(null);
        }

        return VoteData.builder()
                .upvotes((int) upvotes)
                .downvotes((int) downvotes)
                .score((int) (upvotes - downvotes))
                .userVote(userVote)
                .build();
    }

    private Map<Long, Long> getCommentCountsForAlerts(List<Long> alertIds) {
        return alertIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        alertId -> commentRepository.countByAlertIdAndStatus(alertId, CommentStatus.ACTIVE)
                ));
    }

    private EnhancedAlertResponse mapToEnhancedAlertResponse(MissingAlert alert, Long currentUserId,
                                                             Map<Long, VoteData> voteDataMap, Map<Long, Long> commentCountMap) {
        VoteData voteData = voteDataMap.get(alert.getId());
        Long commentCount = commentCountMap.get(alert.getId());
        return mapToEnhancedAlertResponse(alert, currentUserId, voteData, commentCount != null ? commentCount : 0L);
    }

    private EnhancedAlertResponse mapToEnhancedAlertResponse(MissingAlert alert, Long currentUserId,
                                                             VoteData voteData, long commentCount) {
        // Check permissions
        boolean canEdit = currentUserId != null && alert.getPostedBy().getId().equals(currentUserId);
        boolean canDelete = canEdit; // For now, same permissions
        boolean canReport = currentUserId != null && !alert.getPostedBy().getId().equals(currentUserId);

        // Calculate engagement score (simple formula)
        double engagementScore = calculateEngagementScore(voteData.getScore(), (int) commentCount, alert.getCreatedAt());

        // Get user info with additional stats
        User author = alert.getPostedBy();
        long userAlertCount = alertRepository.countByPostedById(author.getId());

        return EnhancedAlertResponse.builder()
                .id(alert.getId())
                .title(alert.getTitle())
                .description(alert.getDescription())
                .location(alert.getLocation())
                .imageUrl(alert.getImageUrl())
                .status(alert.getStatus())
                .reportCount(alert.getReportCount())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .foundAt(alert.getFoundAt())
                .postedBy(EnhancedAlertResponse.UserInfo.builder()
                        .id(author.getId())
                        .name(author.getName())
                        .email(author.getEmail())
                        .reputation(calculateUserReputation(author.getId()))
                        .alertCount((int) userAlertCount)
                        .build())
                .upvotes(voteData.getUpvotes())
                .downvotes(voteData.getDownvotes())
                .score(voteData.getScore())
                .commentCount((int) commentCount)
                .userVote(voteData.getUserVote())
                .canEdit(canEdit)
                .canDelete(canDelete)
                .canReport(canReport)
                .engagementScore(engagementScore)
                .trending(false) // Will be set by specific methods
                .visibility(determineVisibility(alert))
                .build();
    }

    private double calculateEngagementScore(int score, int commentCount, LocalDateTime createdAt) {
        // Simple engagement score: (votes + comments) / age in hours
        long hoursOld = java.time.Duration.between(createdAt, LocalDateTime.now()).toHours();
        hoursOld = Math.max(1, hoursOld); // Prevent division by zero

        return (double) (score + commentCount * 2) / hoursOld;
    }

    private Integer calculateUserReputation(Long userId) {
        // Simple reputation calculation
        List<MissingAlert> userAlerts = alertRepository.findByPostedByIdOrderByCreatedAtDesc(userId);
        return userAlerts.stream()
                .mapToInt(alert -> alert.getUpvotes() - alert.getDownvotes())
                .sum();
    }

    private EnhancedAlertResponse.AlertVisibility determineVisibility(MissingAlert alert) {
        if (alert.isAutoDeleted()) {
            return EnhancedAlertResponse.AlertVisibility.ARCHIVED;
        }
        if (alert.isFlagged()) {
            return EnhancedAlertResponse.AlertVisibility.FLAGGED;
        }
        if (alert.getStatus() == AlertStatus.CLOSED) {
            return EnhancedAlertResponse.AlertVisibility.ARCHIVED;
        }
        return EnhancedAlertResponse.AlertVisibility.PUBLIC;
    }

    // Helper class for vote data
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class VoteData {
        private Integer upvotes;
        private Integer downvotes;
        private Integer score;
        private VoteType userVote;
    }
}