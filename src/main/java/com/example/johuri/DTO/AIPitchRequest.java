package com.example.johuri.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIPitchRequest {

    @NotBlank(message = "Brand name must not be blank")
    private String brandName;

    private String category;
    private Long budgetBDT;
    private String targetAudience;
    private String customDeliverables;
}

