package com.example.johuri.DTO;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import com.example.johuri.entity.CreatorProfile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaKitPublicResponse {

    private String name;
    private String youtubeHandle;
    private String verificationStatus;
    private String avatarUrl;
    private java.util.Map<String, Long> rateCard;
    private CreatorProfile.AIEvaluation aiEvaluation;

    @Valid
    private PublicStatsResponse publicStats;

    @Valid
    private List<ServicePackageResponse> servicePackages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PublicStatsResponse {
        private Long subscriberCount;
        private Long totalViews;
        private Long totalVideos;
        private Double avgViews;
        private Double engagementRate;
        private Double audienceSentimentScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServicePackageResponse {
        private String title;
        private String description;
        private Double priceBDT;
    }
}
