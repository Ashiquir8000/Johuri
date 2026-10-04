package com.example.johuri.DTO;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaKitPublicResponse {

    private String name;
    private String youtubeHandle;
    private String verificationStatus;

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
