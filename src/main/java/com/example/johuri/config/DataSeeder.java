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
@org.springframework.core.annotation.Order(2)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String CREATOR_USER_ID = "dev-demo-rifat-user";
    private static final String BRAND_USER_ID = "dev-demo-brand-user";
    private static final String CREATOR_HANDLE = "@rifat_creator";
    private static final String UNCLAIMED_HANDLE = "@RafsanTheChotoBhai";

    private final UserRepository userRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final DealRepository dealRepository;
    private final com.example.johuri.repository.CampaignDealRepository campaignDealRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Provision Default Admin Account if missing
        if (userRepository.findByEmailIgnoreCase("admin@johuri.ai").isEmpty()) {
            User admin = User.builder()
                    .id("admin-johuri-master")
                    .email("admin@johuri.ai")
                    .fullName("Johuri Super Admin")
                    .name("Johuri Super Admin")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(User.Role.ADMIN)
                    .roles(List.of("ROLE_ADMIN"))
                    .createdAt(Instant.now())
                    .build();
            userRepository.save(admin);
            log.info("Provisioned default admin user: admin@johuri.ai / Admin@123");
        }

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
                        .engagementRate(8.4)
                        .audienceSentimentScore(9.1)
                        .build())
                .creatorRateCard(java.util.Map.of("video", 45000L, "shorts", 20000L, "story", 10000L))
                .aiEvaluation(CreatorProfile.AIEvaluation.builder()
                        .fairMarketValueMin(35000L)
                        .fairMarketValueMax(50000L)
                        .recommendedValuationTag("MID_TIER_OPTIMAL_ROI")
                        .performanceSummary("High consistent views with strong conversion intent among young professionals in Bangladesh.")
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
                        .engagementRate(9.8)
                        .audienceSentimentScore(9.4)
                        .build())
                .creatorRateCard(java.util.Map.of("video", 250000L, "shorts", 120000L, "story", 50000L))
                .aiEvaluation(CreatorProfile.AIEvaluation.builder()
                        .fairMarketValueMin(220000L)
                        .fairMarketValueMax(280000L)
                        .recommendedValuationTag("MACRO_IMPACT_PREMIUM")
                        .performanceSummary("Top-tier lifestyle reach in Bangladesh with multi-million impression retention.")
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

        seedCampaignDealIfEmpty();

        log.info("Development demo data is present");
    }

    private void seedCampaignDealIfEmpty() {
        if (campaignDealRepository.count() == 0) {
            com.example.johuri.entity.CampaignDeal sample = com.example.johuri.entity.CampaignDeal.builder()
                    .dealId("JD-DEMO01")
                    .brandName("Pathao")
                    .creatorHandle("@RafsanTheChotoBhai")
                    .budgetBDT(50_000L)
                    .deliverableType("60s Dedicated Review")
                    .status(com.example.johuri.entity.CampaignDeal.Status.ESCROW_LOCKED)
                    .transactionId("TRXDEMO998811")
                    .bkashSessionId("BKASH-DEMO-SESS-01")
                    .createdAt(Instant.now())
                    .build();
            campaignDealRepository.save(sample);
            log.info("Seeded initial escrow CampaignDeal JD-DEMO01 into MongoDB");
        }

        if (campaignDealRepository.findByDealId("JD-DISPUTE-01").isEmpty()) {
            com.example.johuri.entity.CampaignDeal dispute = com.example.johuri.entity.CampaignDeal.builder()
                    .dealId("JD-DISPUTE-01")
                    .brandName("Chaldal")
                    .creatorHandle("@RafsanTheChotoBhai")
                    .budgetBDT(35_000L)
                    .deliverableType("1x Dedicated Video Review")
                    .status(com.example.johuri.entity.CampaignDeal.Status.DISPUTED)
                    .disputeReason("Brand states sponsor shoutout was cut short to 15 seconds instead of agreed 60 seconds.")
                    .proofLink("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                    .transactionId("TRXDISP992211")
                    .bkashSessionId("BKASH-DISP-SESS-01")
                    .createdAt(Instant.now().minusSeconds(86400 * 2))
                    .build();
            campaignDealRepository.save(dispute);
            log.info("Seeded sample disputed deal JD-DISPUTE-01 for Admin Portal");
        }

        if (campaignDealRepository.findByDealId("JD-SUBMIT-01").isEmpty()) {
            com.example.johuri.entity.CampaignDeal submitted = com.example.johuri.entity.CampaignDeal.builder()
                    .dealId("JD-SUBMIT-01")
                    .brandName("Walton")
                    .creatorHandle("@rifat_creator")
                    .budgetBDT(45_000L)
                    .deliverableType("YouTube Integration (60s)")
                    .status(com.example.johuri.entity.CampaignDeal.Status.DELIVERABLE_SUBMITTED)
                    .proofLink("https://www.youtube.com/watch?v=7h2Y6hV20qY")
                    .transactionId("TRXSUB881122")
                    .bkashSessionId("BKASH-SUB-SESS-01")
                    .createdAt(Instant.now().minusSeconds(86400 * 1))
                    .build();
            campaignDealRepository.save(submitted);
            log.info("Seeded sample deliverable submitted deal JD-SUBMIT-01 for Admin Portal");
        }
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
