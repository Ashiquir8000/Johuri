package com.example.johuri.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIRateEvaluationRequest {

    private String creatorId;
    private String creatorHandle;

    private String brandEmailBody;

    private MetricsPayload metrics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MetricsPayload {
        @PositiveOrZero
        private Long subscriberCount;

        @DecimalMin("0.0")
        private Double avgViews;

        @DecimalMin("0.0")
        private Double engagementRate; // e.g. 5.5 (%)

        @DecimalMin("0.0")
        private Double audienceSentimentScore; // e.g. 8.8 (out of 10)

        private Map<String, Object> creatorRateCard; // e.g. {"video": 50000, "shorts": 25000}
    }
}

