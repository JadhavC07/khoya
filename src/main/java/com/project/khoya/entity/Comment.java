package com.project.khoya.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "comments")
@EntityListeners(AuditingEntityListener.class)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private MissingAlert alert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    // Self-referencing for replies
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> replies = new ArrayList<>();

    // Vote counts for performance
    @Column(name = "upvotes", nullable = false)
    private Integer upvotes = 0;

    @Column(name = "downvotes", nullable = false)
    private Integer downvotes = 0;

    @Column(name = "score", nullable = false)
    private Integer score = 0; // upvotes - downvotes

    @Column(name = "reply_count", nullable = false)
    private Integer replyCount = 0;

    @Column(name = "is_edited", nullable = false)
    private Boolean isEdited = false;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Enumerated(EnumType.STRING)
    private CommentStatus status = CommentStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vote> votes = new ArrayList<>();


    public void incrementUpvotes() {
        this.upvotes++;
        updateScore();
    }

    public void decrementUpvotes() {
        this.upvotes = Math.max(0, this.upvotes - 1);
        updateScore();
    }

    public void incrementDownvotes() {
        this.downvotes++;
        updateScore();
    }

    public void decrementDownvotes() {
        this.downvotes = Math.max(0, this.downvotes - 1);
        updateScore();
    }

    private void updateScore() {
        this.score = this.upvotes - this.downvotes;
    }

    public void incrementReplyCount() {
        this.replyCount++;
    }

    public void decrementReplyCount() {
        this.replyCount = Math.max(0, this.replyCount - 1);
    }

    // Check if this is a root comment
    public boolean isRootComment() {
        return this.parent == null;
    }

    // Get the root comment
    public Comment getRootComment() {
        Comment current = this;
        while (current.getParent() != null) {
            current = current.getParent();
        }
        return current;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

