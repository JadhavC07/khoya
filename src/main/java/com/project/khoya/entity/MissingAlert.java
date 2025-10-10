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
@Table(name = "missing_alerts")
@EntityListeners(AuditingEntityListener.class)
public class MissingAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    private String location;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private AlertStatus status = AlertStatus.ACTIVE;

    private int reportCount = 0;


    @Column(name = "is_flagged", nullable = false)
    private Boolean isFlagged = false;

    @Column(name = "flagged_at")
    private LocalDateTime flaggedAt;

    @Column(name = "flagged_reason")
    private String flaggedReason;

    @Column(name = "auto_deleted", nullable = false)
    private Boolean autoDeleted = false;

    @Column(name = "auto_deleted_at")
    private LocalDateTime autoDeletedAt;


    @Column(name = "upvotes", nullable = false)
    private Integer upvotes = 0;

    @Column(name = "downvotes", nullable = false)
    private Integer downvotes = 0;

    @Column(name = "score", nullable = false)
    private Integer score = 0;

    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0;


    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private LocalDateTime foundAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User postedBy;

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AlertReport> reports;

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Vote> votes = new ArrayList<>();

    @Lob
    @Column(columnDefinition = "BLOB")
    private byte[] imageEmbedding;


    public boolean isFlagged() {
        return Boolean.TRUE.equals(this.isFlagged);
    }

    public boolean isAutoDeleted() {
        return Boolean.TRUE.equals(this.autoDeleted);
    }


    public Boolean getFlagged() {
        return this.isFlagged;
    }


    public void setFlagged(Boolean flagged) {
        this.isFlagged = flagged;
    }



    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }

        if (isFlagged == null) {
            isFlagged = false;
        }
        if (autoDeleted == null) {
            autoDeleted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}