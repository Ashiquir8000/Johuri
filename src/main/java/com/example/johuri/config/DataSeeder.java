package com.example.johuri.config;

import com.example.johuri.entity.CreatorProfile;
import com.example.johuri.entity.Deal;
import com.example.johuri.entity.User;
import com.example.johuri.repository.CreatorProfileRepository;
import com.example.johuri.repository.DealRepository;
import com.example.johuri.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String CREATOR_USER_ID = "dev-demo-rifat-user";
    private static final String BRAND_USER_ID = "dev-demo-brand-user";
    private static final String CREATOR_HANDLE = "@rifat_creator";
    private static final String UNCLAIMED_HANDLE = "@RafsanTheChotoBhai";

    private final UserRepository userRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final DealRepository dealRepository;

    @Override
    public void run(String... args) {
        User creator = saveUserIfMissing(User.builder()
                .id(CREATOR_USER_ID)
                .email("rifat_creator@example.com")
                .name("Rifat Creator")
                .roles(List.of("ROLE_CREATOR"))
                .connectedYouTubeChannelId("UC_DEMO_RIFAT")
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .build());

        saveUserIfMissing(User.builder()
                .id(BRAND_USER_ID)
                .email("brand@example.com")
                .name("Demo Brand")
                .roles(List.of("ROLE_BRAND"))
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .build());

        saveCreatorProfileIfMissing(CREATOR_HANDLE, CreatorProfile.builder()
                .id("dev-demo-rifat-profile")
                .userId(creator.getId())
                .youtubeHandle(CREATOR_HANDLE)
                .verificationStatus(CreatorProfile.VerificationStatus.VERIFIED)
                .publicStats(CreatorProfile.PublicStats.builder()
                        .subscriberCount(125_000L)
                        .totalViews(8_400_000L)
                        .totalVideos(240L)
                        .avgViews(35_000.0)
                        .build())
                .servicePackages(List.of(
                        CreatorProfile.ServicePackage.builder()
                                .title("YouTube Integration")
                                .description("60-second sponsor integration in a long-form video")
                                .priceBDT(45_000.0)
                                .build(),
                        CreatorProfile.ServicePackage.builder()
                                .title("Dedicated Video")
                                .description("Dedicated sponsored video with agreed campaign messaging")
                                .priceBDT(120_000.0)
                                .build()
                ))
                .build());

        saveCreatorProfileIfMissing(UNCLAIMED_HANDLE, CreatorProfile.builder()
                .id("dev-demo-unclaimed-profile")
                .youtubeHandle(UNCLAIMED_HANDLE)
                .verificationStatus(CreatorProfile.VerificationStatus.UNCLAIMED)
                .publicStats(CreatorProfile.PublicStats.builder()
                        .subscriberCount(1_250_000L)
                        .totalViews(185_000_000L)
                        .totalVideos(520L)
                        .avgViews(355_769.0)
                        .build())
                .servicePackages(List.of())
                .build());

        saveDealIfMissing(Deal.builder()
                .id("dev-demo-deal-discussion")
                .creatorId(creator.getId())
                .brandName("Demo Brand")
                .brandEmail("brand@example.com")
                .agreedAmountBDT(45_000.0)
                .status(Deal.Status.IN_DISCUSSION)
                .createdAt(Instant.parse("2025-01-10T10:00:00Z"))
                .build());

        saveDealIfMissing(Deal.builder()
                .id("dev-demo-deal-scripting")
                .creatorId(creator.getId())
                .brandName("Sample Technology")
                .brandEmail("partnerships@example.com")
                .agreedAmountBDT(75_000.0)
                .status(Deal.Status.SCRIPTING)
                .createdAt(Instant.parse("2025-01-12T10:00:00Z"))
                .build());

        log.info("Development demo data is present");
    }

    private User saveUserIfMissing(User user) {
        return userRepository.findById(user.getId())
                .or(() -> userRepository.findByEmailIgnoreCase(user.getEmail()))
                .orElseGet(() -> userRepository.save(user));
    }

    private void saveCreatorProfileIfMissing(String handle, CreatorProfile profile) {
        if (creatorProfileRepository.findByYoutubeHandleIgnoreCase(handle).isEmpty()) {
            creatorProfileRepository.save(profile);
        }
    }

    private void saveDealIfMissing(Deal deal) {
        if (!dealRepository.existsById(deal.getId())) {
            dealRepository.save(deal);
        }
    }
}
