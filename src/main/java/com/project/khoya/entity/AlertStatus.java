package com.project.khoya.entity;

import lombok.Getter;

@Getter
public enum AlertStatus {
    ACTIVE("Active - Currently searching"),
    FOUND("Found - Person has been located"),
    CLOSED("Closed - Search discontinued"),
    UNDER_REVIEW("Under Review - Being investigated");


    private final String description;

    AlertStatus(String description) {
        this.description = description;
    }

}
