package com.example.johuri.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AISmartPitchRequest {

    private String creatorId;
    private String creatorHandle;

    @NotBlank(message = "Brand name must not be blank")
    private String brandName;

    @NotNull(message = "Offered budget must not be null")
    @Positive(message = "Budget must be greater than zero")
    private Long offeredBudgetBDT;

    private String campaignGoals; // e.g. "Brand Awareness", "Conversions", "App Installs"
    private String category; // e.g. "Tech & Gadgets", "Lifestyle", "FinTech"
    private String targetAudience;
    private String customDeliverables; // e.g. "1x Dedicated Video", "1x Short + Pinned Link"
}

