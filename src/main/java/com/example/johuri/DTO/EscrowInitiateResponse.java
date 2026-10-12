package com.example.johuri.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscrowInitiateResponse {

    private String dealId;
    private String bkashSessionId;
    private String merchantNumber;
    private Long amountBDT;
    private String brandName;
    private String creatorHandle;
    private String status;
}

