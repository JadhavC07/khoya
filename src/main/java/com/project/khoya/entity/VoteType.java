package com.project.khoya.entity;

import lombok.Getter;

@Getter
public enum VoteType {
    UP("Upvote"),
    DOWN("Downvote");

    private final String description;

    VoteType(String description) {
        this.description = description;
    }

}

