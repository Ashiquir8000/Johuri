package com.example.johuri.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscrowConfirmResponse {

    private String dealId;
    private String status;
    private Long amountBDT;
    private String transactionId;
    private String creatorHandle;
    private String brandName;
    private String message;
    private Instant lockedAt;
}

