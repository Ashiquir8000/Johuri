package com.example.johuri.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YouTubeTelemetryDTO {
    private String channelId;
    private String title;
    private String handle;
    private String description;
    private String avatarUrl;
    
    private Long subscriberCount;
    private Long viewCount;
    private Long videoCount;
    
    private List<TopVideoDTO> topVideos;
    private List<MediaKitPublicResponse.ServicePackageResponse> servicePackages;
    private java.util.Map<String, Long> rateCard;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopVideoDTO {
        private String videoId;
        private String title;
        private String thumbnailUrl;
    }
}

