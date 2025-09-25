package com.project.khoya.entity;

import lombok.Getter;

@Getter
public enum CommentStatus {
    ACTIVE("Active comment"),
    HIDDEN("Hidden by moderator"),
    DELETED("Deleted by user"),
    FLAGGED("Flagged for review");

    private final String description;

    CommentStatus(String description) {
        this.description = description;
    }

}
