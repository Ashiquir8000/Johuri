package com.example.johuri.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIRateEvaluationResponse {

    private String creatorHandle;
    private Long fairMarketValueMin;
    private Long fairMarketValueMax;
    private Long baselineEstimatedValuationBDT;
    private String recommendedValuationTag; // e.g. "TIER_A_HIGH_ROI", "RISING_STAR_HIGH_ENGAGEMENT"
    private String performanceSummary;
    private Double engagementRate;
    private Double audienceSentimentScore;
    private Double avgViews;
    private Map<String, Object> creatorRateCard;
    private String marketAnalysis;
    private boolean mockResponse;
}

