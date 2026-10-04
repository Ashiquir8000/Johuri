package com.example.johuri.service;

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

@Service
@RequiredArgsConstructor
public class CreatorService {

    private final CreatorProfileRepository creatorProfileRepository;
    private final UserRepository userRepository;
    private final YouTubeDataService youTubeDataService;

    public MediaKitPublicResponse getPublicChannelTelemetry(String handle) {
        String normalizedHandle = normalizeHandle(handle);
        CreatorProfile.PublicStats stats = youTubeDataService.fetchPublicStatistics(normalizedHandle);
        CreatorProfile profile = findProfile(normalizedHandle);

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
        String creatorName = profile.getUserId() == null
                ? null
                : userRepository.findById(profile.getUserId()).map(User::getName).orElse(null);

        CreatorProfile.PublicStats publicStats = profile.getPublicStats();
        return MediaKitPublicResponse.builder()
                .name(creatorName)
                .youtubeHandle(profile.getYoutubeHandle())
                .verificationStatus(profile.getVerificationStatus().name())
                .publicStats(publicStats == null ? null : MediaKitPublicResponse.PublicStatsResponse.builder()
                        .subscriberCount(publicStats.getSubscriberCount())
                        .totalViews(publicStats.getTotalViews())
                        .totalVideos(publicStats.getTotalVideos())
                        .avgViews(publicStats.getAvgViews())
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
