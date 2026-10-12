package com.example.johuri.service;

import com.example.johuri.DTO.AIPitchRequest;
import com.example.johuri.DTO.AIPitchResponse;
import com.example.johuri.DTO.CreatorProfileClaimRequest;
import com.example.johuri.DTO.CreatorProfileClaimResponse;
import com.example.johuri.DTO.MediaKitPublicResponse;
import com.example.johuri.entity.CreatorProfile;
import com.example.johuri.entity.User;
import com.example.johuri.repository.CreatorProfileRepository;
import com.example.johuri.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreatorService {

    private final CreatorProfileRepository creatorProfileRepository;
    private final UserRepository userRepository;
    private final YouTubeDataService youTubeDataService;
    private final GeminiAIService geminiAIService;

    public List<MediaKitPublicResponse> getCreators() {
        Map<String, String> creatorNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getName, (first, ignored) -> first));

        return creatorProfileRepository.findAll().stream()
                .map(profile -> toPublicMediaKit(profile, creatorNames))
                .toList();
    }

    public MediaKitPublicResponse getCreatorByHandle(String handle) {
        String normalizedHandle = normalizeHandle(handle);
        CreatorProfile profile = findProfile(normalizedHandle);
        CreatorProfile.PublicStats stats = null;

        try {
            stats = youTubeDataService.fetchPublicStatistics(normalizedHandle);
        } catch (Exception ignored) {
            // If YouTube API fails or channel not found, use stored stats from DB
        }

        if (profile == null) {
            profile = CreatorProfile.builder()
                    .youtubeHandle(normalizedHandle)
                    .verificationStatus(CreatorProfile.VerificationStatus.UNCLAIMED)
                    .publicStats(stats != null ? stats : CreatorProfile.PublicStats.builder()
                            .subscriberCount(0L)
                            .totalViews(0L)
                            .totalVideos(0L)
                            .avgViews(0.0)
                            .build())
                    .build();
        } else if (stats != null) {
            profile.setPublicStats(stats);
            if (profile.getVerificationStatus() == null) {
                profile.setVerificationStatus(CreatorProfile.VerificationStatus.UNCLAIMED);
            }
        }

        profile = creatorProfileRepository.save(profile);
        String creatorName = profile.getUserId() == null
                ? null
                : userRepository.findById(profile.getUserId()).map(User::getName).orElse(null);
        return toPublicMediaKit(profile, creatorName);
    }

    public MediaKitPublicResponse getPublicChannelTelemetry(String handle) {
        return getCreatorByHandle(handle);
    }

    public com.example.johuri.DTO.YouTubeTelemetryDTO syncCreatorTelemetry(String handle) {
        String normalizedHandle = normalizeHandle(handle);
        com.example.johuri.DTO.YouTubeTelemetryDTO telemetry = null;
        try {
            telemetry = youTubeDataService.fetchCreatorTelemetry(normalizedHandle);
        } catch (Exception ignored) {}

        CreatorProfile profile = findProfile(normalizedHandle);
        CreatorProfile.PublicStats stats = null;
        if (telemetry != null) {
            stats = CreatorProfile.PublicStats.builder()
                    .subscriberCount(telemetry.getSubscriberCount())
                    .totalViews(telemetry.getViewCount())
                    .totalVideos(telemetry.getVideoCount())
                    .avgViews(telemetry.getViewCount() != null && telemetry.getVideoCount() != null && telemetry.getVideoCount() > 0 
                            ? (double) telemetry.getViewCount() / telemetry.getVideoCount() : 0.0)
                    .build();
        } else if (profile != null && profile.getPublicStats() != null) {
            stats = profile.getPublicStats();
            telemetry = com.example.johuri.DTO.YouTubeTelemetryDTO.builder()
                    .handle(normalizedHandle)
                    .subscriberCount(stats.getSubscriberCount())
                    .viewCount(stats.getTotalViews())
                    .videoCount(stats.getTotalVideos())
                    .topVideos(java.util.Collections.emptyList())
                    .build();
        } else {
            stats = CreatorProfile.PublicStats.builder()
                    .subscriberCount(0L)
                    .totalViews(0L)
                    .totalVideos(0L)
                    .avgViews(0.0)
                    .build();
            telemetry = com.example.johuri.DTO.YouTubeTelemetryDTO.builder()
                    .handle(normalizedHandle)
                    .subscriberCount(0L)
                    .viewCount(0L)
                    .videoCount(0L)
                    .topVideos(java.util.Collections.emptyList())
                    .build();
        }
                
        if (profile == null) {
            profile = CreatorProfile.builder()
                    .youtubeHandle(normalizedHandle)
                    .verificationStatus(CreatorProfile.VerificationStatus.UNCLAIMED)
                    .publicStats(stats)
                    .build();
        } else {
            profile.setPublicStats(stats);
            if (profile.getVerificationStatus() == null) {
                profile.setVerificationStatus(CreatorProfile.VerificationStatus.UNCLAIMED);
            }
        }
        profile = creatorProfileRepository.save(profile);

        // Attach saved service packages and rate card to telemetry DTO
        if (profile.getServicePackages() != null && !profile.getServicePackages().isEmpty()) {
            telemetry.setServicePackages(profile.getServicePackages().stream()
                    .map(sp -> MediaKitPublicResponse.ServicePackageResponse.builder()
                            .title(sp.getTitle())
                            .description(sp.getDescription())
                            .priceBDT(sp.getPriceBDT())
                            .build())
                    .toList());
            
            java.util.Map<String, Long> rateCardMap = new java.util.HashMap<>();
            for (CreatorProfile.ServicePackage sp : profile.getServicePackages()) {
                String title = sp.getTitle().toLowerCase();
                if (title.contains("video") || title.contains("dedicated") || title.contains("integration")) {
                    rateCardMap.put("video", sp.getPriceBDT() != null ? sp.getPriceBDT().longValue() : 0L);
                } else if (title.contains("short") || title.contains("reel")) {
                    rateCardMap.put("shorts", sp.getPriceBDT() != null ? sp.getPriceBDT().longValue() : 0L);
                } else if (title.contains("story") || title.contains("community") || title.contains("post")) {
                    rateCardMap.put("story", sp.getPriceBDT() != null ? sp.getPriceBDT().longValue() : 0L);
                }
            }
            telemetry.setRateCard(rateCardMap);
        }
        
        return telemetry;
    }

    private MediaKitPublicResponse toPublicMediaKit(CreatorProfile profile, String creatorName) {
        CreatorProfile.VerificationStatus verificationStatus = profile.getVerificationStatus() == null
                ? CreatorProfile.VerificationStatus.UNCLAIMED
                : profile.getVerificationStatus();
        CreatorProfile.PublicStats publicStats = profile.getPublicStats();
        
        java.util.Map<String, Long> rateCardMap = new java.util.HashMap<>();
        if (profile.getServicePackages() != null) {
            for (CreatorProfile.ServicePackage sp : profile.getServicePackages()) {
                String title = sp.getTitle().toLowerCase();
                if (title.contains("video") || title.contains("dedicated") || title.contains("integration")) {
                    rateCardMap.put("video", sp.getPriceBDT() != null ? sp.getPriceBDT().longValue() : 0L);
                } else if (title.contains("short") || title.contains("reel")) {
                    rateCardMap.put("shorts", sp.getPriceBDT() != null ? sp.getPriceBDT().longValue() : 0L);
                } else if (title.contains("story") || title.contains("community") || title.contains("post")) {
                    rateCardMap.put("story", sp.getPriceBDT() != null ? sp.getPriceBDT().longValue() : 0L);
                }
            }
        }

        return MediaKitPublicResponse.builder()
                .name(creatorName)
                .youtubeHandle(profile.getYoutubeHandle())
                .verificationStatus(verificationStatus.name())
                .rateCard(rateCardMap)
                .aiEvaluation(profile.getAiEvaluation())
                .publicStats(publicStats == null ? null : MediaKitPublicResponse.PublicStatsResponse.builder()
                        .subscriberCount(publicStats.getSubscriberCount())
                        .totalViews(publicStats.getTotalViews())
                        .totalVideos(publicStats.getTotalVideos())
                        .avgViews(publicStats.getAvgViews())
                        .engagementRate(publicStats.getEngagementRate())
                        .audienceSentimentScore(publicStats.getAudienceSentimentScore())
                        .build())
                .servicePackages(profile.getServicePackages() == null
                        ? List.of()
                        : profile.getServicePackages().stream()
                        .map(servicePackage -> MediaKitPublicResponse.ServicePackageResponse.builder()
                                .title(servicePackage.getTitle())
                                .description(servicePackage.getDescription())
                                .priceBDT(servicePackage.getPriceBDT())
                                .build())
                        .toList())
                .build();
    }

    private MediaKitPublicResponse toPublicMediaKit(
            CreatorProfile profile,
            Map<String, String> creatorNames) {
        String creatorName = profile.getUserId() == null ? null : creatorNames.get(profile.getUserId());
        return toPublicMediaKit(profile, creatorName);
    }

    public CreatorProfileClaimResponse claimProfile(CreatorProfileClaimRequest request) {
        String normalizedHandle = normalizeHandle(request.getYoutubeHandle());
        User user = userRepository.findByEmailIgnoreCase(request.getGoogleAccountEmail().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Google account does not match a registered user"
                ));

        CreatorProfile profile = findProfile(normalizedHandle);
        if (profile == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Creator profile was not found");
        }
        if (profile.getVerificationStatus() != CreatorProfile.VerificationStatus.UNCLAIMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Creator profile is not available to claim");
        }
        if (profile.getUserId() != null && !profile.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Creator profile is linked to another user");
        }
        if (user.getConnectedYouTubeChannelId() != null
                && !user.getConnectedYouTubeChannelId().equals(request.getYoutubeChannelId().trim())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A different YouTube channel is already linked to this user"
            );
        }

        // Mock verification only: this matches the supplied Google email to the local account.
        // It does not prove YouTube channel ownership; replace with OAuth verification before production.
        user.setConnectedYouTubeChannelId(request.getYoutubeChannelId().trim());
        userRepository.save(user);

        profile.setUserId(user.getId());
        profile.setVerificationStatus(CreatorProfile.VerificationStatus.VERIFIED);
        profile = creatorProfileRepository.save(profile);

        return CreatorProfileClaimResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .youtubeHandle(profile.getYoutubeHandle())
                .verificationStatus(profile.getVerificationStatus().name())
                .build();
    }

    private CreatorProfile findProfile(String handle) {
        List<CreatorProfile> matches = creatorProfileRepository.findByYoutubeHandleIgnoreCase(handle);
        if (matches.isEmpty()) {
            matches = creatorProfileRepository.findByYoutubeHandleIgnoreCase(handle.substring(1));
        }
        if (matches.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Multiple creator profiles match this handle");
        }
        return matches.isEmpty() ? null : matches.get(0);
    }

    public AIPitchResponse generatePitch(String handle, AIPitchRequest request) {
        String normalizedHandle = normalizeHandle(handle);
        CreatorProfile profile = findProfile(normalizedHandle);
        CreatorProfile.PublicStats stats = null;

        if (profile != null && profile.getPublicStats() != null) {
            stats = profile.getPublicStats();
        } else {
            try {
                stats = youTubeDataService.fetchPublicStatistics(normalizedHandle);
            } catch (Exception e) {
                // Ignore and use sensible defaults if YouTube API is unavailable
            }
        }

        if (stats == null) {
            stats = CreatorProfile.PublicStats.builder()
                    .subscriberCount(0L)
                    .totalViews(0L)
                    .totalVideos(0L)
                    .avgViews(0.0)
                    .build();
        }

        String niche = (request != null && request.getCategory() != null && !request.getCategory().isBlank())
                ? request.getCategory().trim()
                : "Content Creation & Lifestyle";

        return geminiAIService.generateCampaignPitch(normalizedHandle, stats, niche, request);
    }

    private String normalizeHandle(String handle) {
        if (handle == null || handle.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "YouTube handle must not be blank");
        }

        String normalized = handle.trim();
        while (normalized.startsWith("@")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "YouTube handle must not be blank");
        }
        return "@" + normalized;
    }
}
