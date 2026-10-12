package com.example.johuri.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscrowInitiateRequest {

    @NotBlank(message = "Brand name must not be blank")
    private String brandName;

    @NotBlank(message = "Creator handle must not be blank")
    private String creatorHandle;

    @NotNull(message = "Budget in BDT is required")
    private Long budgetBDT;

    private String deliverableType;
}

