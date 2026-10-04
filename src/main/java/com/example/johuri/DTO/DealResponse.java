package com.example.johuri.DTO;

import com.example.johuri.entity.Deal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DealResponse {

    private String id;
    private String creatorId;
    private String brandName;
    private String brandEmail;
    private Double agreedAmountBDT;
    private Deal.Status status;
    private String invoicePdfUrl;
    private Instant createdAt;
}
