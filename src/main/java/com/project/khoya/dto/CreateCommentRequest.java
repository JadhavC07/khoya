package com.project.khoya.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request to create a comment")
public class CreateCommentRequest {

    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 1000, message = "Content must be between 1 and 1000 characters")
    @Schema(description = "Comment content", example = "I saw this person near the downtown area yesterday")
    private String content;

    @Schema(description = "Parent comment ID for replies (optional)", example = "1")
    private Long parentId;
}