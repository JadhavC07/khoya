package com.project.khoya.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String status;
    private String message;
    private String error;
    private long timestamp;

    public ErrorResponse(String message, String error) {
        this.status = "error";
        this.message = message;
        this.error = error;
        this.timestamp = System.currentTimeMillis();
    }
}
