package com.project.khoya.entity;

import lombok.Getter;

@Getter
public enum ReportReason {
    FAKE_ALERT("Fake or fraudulent alert"),
    INAPPROPRIATE_CONTENT("Inappropriate or offensive content"),
    SPAM("Spam or repeated posting"),
    MISLEADING_INFO("Misleading or false information"),
    HARASSMENT("Harassment or targeted abuse"),
    PRIVACY_VIOLATION("Privacy violation or unauthorized posting"),
    COMMERCIAL_USE("Commercial or promotional use"),
    OTHER("Other reason");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
