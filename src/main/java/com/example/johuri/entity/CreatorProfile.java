package com.example.johuri.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "creator_profiles")
public class CreatorProfile {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String youtubeHandle;

    private VerificationStatus verificationStatus;
    private PublicStats publicStats;
    private List<ServicePackage> servicePackages;
    private PaymentDetails paymentDetails;

    public enum VerificationStatus {
        UNCLAIMED,
        VERIFIED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PublicStats {
        private Long subscriberCount;
        private Long totalViews;
        private Long totalVideos;
        private Double avgViews;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServicePackage {
        private String title;
        private String description;
        private Double priceBDT;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentDetails {
        private String bkashMerchantNumber;
        private String bankAccount;
    }
}
