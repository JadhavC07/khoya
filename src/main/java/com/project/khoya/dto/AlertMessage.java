package com.project.khoya.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertMessage implements Serializable {
    private Long alertId;
    private String title;
    private String description;
    private String location;
    private String imageUrl;
}