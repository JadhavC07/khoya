package com.project.khoya.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Paginated alert list response")
public class AlertListResponse {

    @Schema(description = "List of alerts")
    private List<AlertResponse> alerts;

    @Schema(description = "Current page number")
    private int page;

    @Schema(description = "Number of items per page")
    private int size;

    @Schema(description = "Total number of alerts")
    private long totalElements;

    @Schema(description = "Total number of pages")
    private int totalPages;

    @Schema(description = "Whether this is the first page")
    private boolean isFirst;

    @Schema(description = "Whether this is the last page")
    private boolean isLast;
}

