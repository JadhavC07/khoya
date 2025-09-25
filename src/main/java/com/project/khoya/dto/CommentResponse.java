package com.project.khoya.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.khoya.entity.CommentStatus;
import com.project.khoya.entity.VoteType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Comment response with vote information")
public class CommentResponse {

    @Schema(description = "Comment ID", example = "1")
    private Long id;

    @Schema(description = "Comment content", example = "I saw this person near downtown yesterday")
    private String content;

    @Schema(description = "Alert ID this comment belongs to", example = "1")
    private Long alertId;

    @Schema(description = "Parent comment ID (for replies)", example = "1")
    private Long parentId;

    @Schema(description = "Number of upvotes", example = "5")
    private Integer upvotes;

    @Schema(description = "Number of downvotes", example = "1")
    private Integer downvotes;

    @Schema(description = "Comment score (upvotes - downvotes)", example = "4")
    private Integer score;

    @Schema(description = "Number of replies", example = "3")
    private Integer replyCount;

    @Schema(description = "Whether comment has been edited", example = "false")
    private Boolean isEdited;

    @Schema(description = "Comment status")
    private CommentStatus status;

    @Schema(description = "When the comment was created")
    private LocalDateTime createdAt;

    @Schema(description = "When the comment was last updated")
    private LocalDateTime updatedAt;

    @Schema(description = "When the comment was edited (if applicable)")
    private LocalDateTime editedAt;

    @Schema(description = "Comment author information")
    private UserInfo author;

    @Schema(description = "Current user's vote on this comment")
    private VoteType userVote;

    @Schema(description = "Whether current user can edit this comment")
    private Boolean canEdit;

    @Schema(description = "Whether current user can delete this comment")
    private Boolean canDelete;

    @Schema(description = "Replies to this comment (for hierarchical display)")
    private List<CommentResponse> replies;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String name;
        private String email;
    }
}

