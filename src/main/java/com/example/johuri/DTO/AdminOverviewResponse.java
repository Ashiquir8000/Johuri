package com.example.johuri.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOverviewResponse {
    private Long totalEscrowLockedBDT;
    private Long completedVolumeBDT;
    private Long disputedDealsCount;
    private Double platformCommissionBDT;
    private Long activeCampaignsCount;
    private Long totalDealsCount;
}

