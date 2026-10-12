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
@Document(collection = "deals")
public class Deal {

    @Id
    private String id;

    @Indexed
    private String creatorId;

    private String brandName;
    private String brandEmail;
    private Double agreedAmountBDT;
    private Status status;
    private String invoicePdfUrl;
    private Instant createdAt;
    private CreatorProfile.AIEvaluation aiEvaluation;
    private CreatorProfile.PublicStats performanceMetrics;

    public enum Status {
        IN_DISCUSSION,
        SCRIPTING,
        EXECUTION,
        INVOICED,
        PAID
    }
}
