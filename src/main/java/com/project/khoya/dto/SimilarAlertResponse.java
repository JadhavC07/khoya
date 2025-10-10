package com.project.khoya.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for similar alert search results.
 * Includes the alert details and its similarity score.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarAlertResponse {

    /**
     * The alert details
     */
    private AlertResponse alert;

    /**
     * Cosine similarity score between 0.0 and 1.0
     * Higher values indicate greater visual similarity
     * - 1.0: Identical images
     * - 0.8-0.99: Very similar (likely same person)
     * - 0.6-0.79: Similar (possibly same person)
     * - 0.4-0.59: Somewhat similar
     * - < 0.4: Different
     */
    private double similarityScore;

    /**
     * Get similarity score as percentage (0-100%)
     */
    public double getSimilarityPercentage() {
        return similarityScore * 100.0;
    }

    /**
     * Get human-readable similarity level
     */
    public String getSimilarityLevel() {
        if (similarityScore >= 0.9) return "VERY_HIGH";
        if (similarityScore >= 0.75) return "HIGH";
        if (similarityScore >= 0.6) return "MEDIUM";
        if (similarityScore >= 0.4) return "LOW";
        return "VERY_LOW";
    }
}