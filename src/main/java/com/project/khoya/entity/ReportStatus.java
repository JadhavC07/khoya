package com.project.khoya.entity;

import lombok.Getter;

@Getter
public enum ReportStatus {
    PENDING("Pending review"),
    VALID("Valid report - action taken"),
    INVALID("Invalid report - no action taken"),
    DISMISSED("Dismissed by admin");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }

}