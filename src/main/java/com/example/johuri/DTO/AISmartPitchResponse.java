package com.example.johuri.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AISmartPitchResponse {

    private String creatorHandle;
    private String brandName;
    private Long offeredBudgetBDT;

    // Data-backed metrics & performance evaluation
    private Long fairMarketValueMin;
    private Long fairMarketValueMax;
    private String budgetFitAssessment; // e.g. "UNDER_BUDGET_FOR_DEDICATED", "OPTIMAL_FIT", "PREMIUM_OVERBID"
    private String dataJustification; // e.g. "Given Creator's avg views of 50k+ over last 5 videos, your ৳30,000 budget fits an Integrated Shoutout, but Dedicated Video Fair Market Value is ৳45,000"

    // Strategic Deliverables Alignment
    private String recommendedDeliverables;
    private List<String> alternativeDeliverableOptions;

    // Complete Sponsorship Pitch & Strategy
    private String projectedReach;
    private String estimatedCPM;
    private String budgetAllocationRationale;

    private String hookAndMatchRationale;
    private String deliverablesStrategy;
    private String projectedRoiAndCta;
    private String fullPitch;
    private String proposalEmailTemplate;

    private boolean mockResponse;
}

