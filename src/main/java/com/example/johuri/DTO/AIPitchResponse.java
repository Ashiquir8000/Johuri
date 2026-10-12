package com.example.johuri.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIPitchResponse {

    private String creatorHandle;
    private String brandName;
    private String hookAndMatchRationale;
    private String deliverablesStrategy;
    private String projectedRoiAndCta;
    private String fullPitch;
    private boolean mockResponse;
}

