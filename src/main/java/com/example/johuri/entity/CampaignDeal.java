package com.example.johuri.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "campaign_deals")
public class CampaignDeal {

    @Id
    private String id;

    @Indexed(unique = true)
    private String dealId;

    private String brandName;
    private String creatorHandle;
    private Long budgetBDT;
    private Status status;
    private String transactionId;
    private String bkashSessionId;
    private Instant createdAt;
    private Instant completedAt;
    private String proofLink;
    private String deliverableType;
    private CreatorProfile.AIEvaluation aiEvaluation;
    private CreatorProfile.PublicStats performanceMetrics;

    private String adminNotes;
    private String disputeReason;
    private Instant resolvedAt;
    private String resolvedBy;
    private String deliverableReviewStatus;
    private java.util.List<AuditEntry> auditTrail;

    public enum Status {
        PENDING,
        ESCROW_LOCKED,
        IN_ESCROW,
        DELIVERABLE_SUBMITTED,
        COMPLETED,
        DISPUTED,
        FORCE_RELEASED,
        REFUNDED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuditEntry {
        private String action;
        private String performedBy;
        private String notes;
        private Instant timestamp;
    }
}

