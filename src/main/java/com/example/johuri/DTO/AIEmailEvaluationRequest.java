package com.example.johuri.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIEmailEvaluationRequest {

    @NotBlank
    @Size(max = 10_000)
    private String brandEmailBody;

    @NotNull
    @Valid
    private CreatorMetrics creatorMetrics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreatorMetrics {

        @NotNull
        @PositiveOrZero
        private Long subscriberCount;

        @NotNull
        @DecimalMin("0.0")
        private Double avgViews;
    }
}
