package com.example.johuri.service;

import com.example.johuri.DTO.AIRateEvaluationRequest;
import com.example.johuri.DTO.AIRateEvaluationResponse;
import com.example.johuri.DTO.AISmartPitchRequest;
import com.example.johuri.DTO.AISmartPitchResponse;
import com.example.johuri.entity.CreatorProfile;
import com.example.johuri.repository.CreatorProfileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AIServiceImpl implements AIService {

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private final String apiKey;
    private final String model;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CreatorProfileRepository creatorProfileRepository;
    private final YouTubeDataService youTubeDataService;

    @Autowired
    public AIServiceImpl(
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.api.model:gemini-2.0-flash}") String model,
            CreatorProfileRepository creatorProfileRepository,
            YouTubeDataService youTubeDataService) {
        this.apiKey = apiKey;
        this.model = model;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.creatorProfileRepository = creatorProfileRepository;
        this.youTubeDataService = youTubeDataService;
    }

    @Override
    public AIRateEvaluationResponse evaluateRate(AIRateEvaluationRequest request) {
        CreatorProfile profile = resolveCreatorProfile(request.getCreatorId(), request.getCreatorHandle());
        CreatorProfile.PublicStats metrics = extractMetrics(request, profile);

        double avgViews = (metrics.getAvgViews() != null && metrics.getAvgViews() > 0) ? metrics.getAvgViews() : 0.0;
        long subs = (metrics.getSubscriberCount() != null && metrics.getSubscriberCount() > 0) ? metrics.getSubscriberCount() : 0L;
        double engagementRate = (metrics.getEngagementRate() != null && metrics.getEngagementRate() > 0)
                ? metrics.getEngagementRate()
                : computeDefaultEngagement(subs, avgViews);
        double sentimentScore = (metrics.getAudienceSentimentScore() != null && metrics.getAudienceSentimentScore() > 0)
                ? metrics.getAudienceSentimentScore()
                : 8.5;

        // If telemetry stats are missing or 0, calculate smart benchmark AI valuation based on Subscriber count tier & profile grade
        long fairMin;
        long fairMax;
        String tag;

        if (avgViews <= 0) {
            // Tier 1 Micro: 5k - 20k subscribers -> BDT 3,500 - 6,000
            // Tier 2 Mid: 20k - 100k subscribers -> BDT 8,000 - 25,000
            // Tier 3 Macro: 100k - 500k subscribers -> BDT 30,000 - 75,000
            // Tier 4 Mega: 500k+ subscribers -> BDT 80,000 - 300,000+
            if (subs <= 20_000) {
                fairMin = Math.max(3_500, Math.round((Math.max(1_000, subs) * 0.35) / 500) * 500);
                fairMax = Math.max(6_000, Math.round((fairMin * 1.5) / 500) * 500);
                tag = "TIER_1_MICRO_COMMUNITY";
                avgViews = Math.max(1_500, Math.round(subs * 0.18));
            } else if (subs <= 100_000) {
                fairMin = Math.max(8_000, Math.round((subs * 0.25) / 1000) * 1000);
                fairMax = Math.max(fairMin + 5_000, Math.round((fairMin * 1.6) / 1000) * 1000);
                tag = "TIER_2_GROWING_INFLUENCE";
                avgViews = Math.round(subs * 0.15);
            } else if (subs <= 500_000) {
                fairMin = Math.max(30_000, Math.round((subs * 0.20) / 5000) * 5000);
                fairMax = Math.max(fairMin + 15_000, Math.round((fairMin * 1.5) / 5000) * 5000);
                tag = "TIER_3_MACRO_AUTHORITY";
                avgViews = Math.round(subs * 0.12);
            } else {
                fairMin = Math.max(80_000, Math.round((subs * 0.15) / 10000) * 10000);
                fairMax = Math.max(fairMin + 40_000, Math.round((fairMin * 1.6) / 10000) * 10000);
                tag = "TIER_4_MEGA_NETWORK";
                avgViews = Math.round(subs * 0.10);
            }
        } else {
            double engagementMultiplier = Math.max(0.8, Math.min(1.5, engagementRate / 6.0));
            long baseDedicatedBDT = Math.round(avgViews * 0.60 * engagementMultiplier);
            fairMin = Math.max(3_500, Math.round((baseDedicatedBDT * 0.85) / 1000) * 1000);
            fairMax = Math.max(fairMin + 5_000, Math.round((baseDedicatedBDT * 1.15) / 1000) * 1000);

            if (engagementRate >= 9.0) {
                tag = "HIGH_ENGAGEMENT_ALPHA";
            } else if (avgViews >= 100_000) {
                tag = "MACRO_IMPACT_PREMIUM";
            } else if (avgViews >= 30_000) {
                tag = "MID_TIER_OPTIMAL_ROI";
            } else {
                tag = "NICHED_HIGH_LOYALTY";
            }
        }

        long baselineVal = Math.round((fairMin + fairMax) / 2.0);

        String performanceSummary = String.format(
                "Creator maintains an average of %s views with a %.1f%% engagement rate and %.1f/10 audience sentiment. Recommended valuation tier: %s.",
                formatNumber(Math.round(avgViews)), engagementRate, sentimentScore, tag
        );

        Map<String, Object> rateCard = (profile != null && profile.getCreatorRateCard() != null)
                ? profile.getCreatorRateCard()
                : (request.getMetrics() != null ? request.getMetrics().getCreatorRateCard() : null);

        String creatorHandle = (profile != null && profile.getYoutubeHandle() != null)
                ? profile.getYoutubeHandle()
                : (request.getCreatorHandle() != null ? request.getCreatorHandle() : "@creator");

        // Try AI prompt with Gemini if API key is present
        String marketAnalysis = null;
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                marketAnalysis = callGeminiForRateAnalysis(creatorHandle, avgViews, engagementRate, sentimentScore, fairMin, fairMax);
            } catch (Exception e) {
                log.warn("Gemini evaluation call failed, using deterministic market evaluation: {}", e.getMessage());
            }
        }

        if (marketAnalysis == null || marketAnalysis.isBlank()) {
            marketAnalysis = String.format(
                    "Market Analysis for %s: Fair market valuation spans ৳%s to ৳%s BDT for dedicated content integration. Given the %.1f%% engagement rate, brand conversions are projected to perform in the top 20th percentile for local Bangladesh audiences.",
                    creatorHandle, formatCurrency(fairMin), formatCurrency(fairMax), engagementRate
            );
        }

        // Persist evaluation back to CreatorProfile if loaded
        if (profile != null) {
            try {
                profile.setAiEvaluation(CreatorProfile.AIEvaluation.builder()
                        .fairMarketValueMin(fairMin)
                        .fairMarketValueMax(fairMax)
                        .recommendedValuationTag(tag)
                        .performanceSummary(performanceSummary)
                        .build());
                if (profile.getPublicStats() != null) {
                    profile.getPublicStats().setEngagementRate(engagementRate);
                    profile.getPublicStats().setAudienceSentimentScore(sentimentScore);
                }
                creatorProfileRepository.save(profile);
            } catch (Exception e) {
                log.warn("Could not save updated AI evaluation to profile: {}", e.getMessage());
            }
        }

        return AIRateEvaluationResponse.builder()
                .creatorHandle(creatorHandle)
                .fairMarketValueMin(fairMin)
                .fairMarketValueMax(fairMax)
                .baselineEstimatedValuationBDT(baselineVal)
                .recommendedValuationTag(tag)
                .performanceSummary(performanceSummary)
                .engagementRate(engagementRate)
                .audienceSentimentScore(sentimentScore)
                .avgViews(avgViews)
                .creatorRateCard(rateCard)
                .marketAnalysis(marketAnalysis)
                .mockResponse(apiKey == null || apiKey.isBlank())
                .build();
    }

    @Override
    public AISmartPitchResponse generateSmartPitch(AISmartPitchRequest request) {
        CreatorProfile profile = resolveCreatorProfile(request.getCreatorId(), request.getCreatorHandle());
        String creatorHandle = (profile != null && profile.getYoutubeHandle() != null)
                ? profile.getYoutubeHandle()
                : (request.getCreatorHandle() != null ? request.getCreatorHandle() : "@creator");

        CreatorProfile.PublicStats stats = (profile != null && profile.getPublicStats() != null)
                ? profile.getPublicStats()
                : CreatorProfile.PublicStats.builder()
                        .subscriberCount(500_000L)
                        .avgViews(50_000.0)
                        .engagementRate(7.5)
                        .audienceSentimentScore(8.6)
                        .build();

        double avgViews = (stats.getAvgViews() != null && stats.getAvgViews() > 0) ? stats.getAvgViews() : 30_000.0;
        double engagementRate = (stats.getEngagementRate() != null && stats.getEngagementRate() > 0)
                ? stats.getEngagementRate()
                : computeDefaultEngagement(stats.getSubscriberCount(), avgViews);

        long offeredBudget = request.getOfferedBudgetBDT();
        long fairDedicatedMin = Math.max(15_000, Math.round((avgViews * 0.50) / 1000) * 1000);
        long fairDedicatedMax = Math.max(fairDedicatedMin + 10_000, Math.round((avgViews * 0.75) / 1000) * 1000);
        long fairIntegratedMin = Math.round(fairDedicatedMin * 0.50);

        // Budget Fit Assessment & Data-Backed Justification
        String budgetFitAssessment;
        String dataJustification;
        String recommendedDeliverables;
        List<String> alternatives = new ArrayList<>();

        if (offeredBudget < fairIntegratedMin) {
            budgetFitAssessment = "BUDGET_BELOW_MARKET";
            dataJustification = String.format(
                    "Given %s's average views of %s+ and %.1f%% engagement, your ৳%s BDT budget is below standard dedicated rates (Fair Range: ৳%s - ৳%s). We recommend adjusting scope to a Community Post or YouTube Short, or increasing budget to ৳%s.",
                    creatorHandle, formatNumber(Math.round(avgViews)), engagementRate,
                    formatCurrency(offeredBudget), formatCurrency(fairDedicatedMin), formatCurrency(fairDedicatedMax),
                    formatCurrency(fairIntegratedMin)
            );
            recommendedDeliverables = "1x YouTube Community Tab Branded Post + Pinned Comment Link";
            alternatives.add("1x YouTube Short (60s vertical) @ ৳" + formatCurrency(fairIntegratedMin));
            alternatives.add("Community Post + Instagram Story bundle @ ৳" + formatCurrency(offeredBudget));
        } else if (offeredBudget < fairDedicatedMin) {
            budgetFitAssessment = "OPTIMAL_FOR_INTEGRATED";
            dataJustification = String.format(
                    "Given %s's average views of %s+ over recent uploads, your ৳%s BDT budget aligns with an Integrated Shoutout (60-90s) and Pinned Link, whereas a full Dedicated Video Fair Market Value is ৳%s - ৳%s BDT.",
                    creatorHandle, formatNumber(Math.round(avgViews)),
                    formatCurrency(offeredBudget), formatCurrency(fairDedicatedMin), formatCurrency(fairDedicatedMax)
            );
            recommendedDeliverables = "1x Integrated Mid-Roll Segment (60-90s) + Pinned UTM Link";
            alternatives.add("2x YouTube Shorts + Pinned Comments");
            alternatives.add("Upgrade to Dedicated Video Review by adjusting budget to ৳" + formatCurrency(fairDedicatedMin));
        } else if (offeredBudget <= fairDedicatedMax) {
            budgetFitAssessment = "OPTIMAL_FIT_FOR_DEDICATED";
            dataJustification = String.format(
                    "Your ৳%s BDT budget is squarely within the Fair Market Value range (৳%s - ৳%s BDT) for %s (avg %s views, %.1f%% engagement). This provides high alignment for a full Dedicated Video Review.",
                    formatCurrency(offeredBudget), formatCurrency(fairDedicatedMin), formatCurrency(fairDedicatedMax),
                    creatorHandle, formatNumber(Math.round(avgViews)), engagementRate
            );
            recommendedDeliverables = "1x Dedicated Video Review (8-12 mins) + Pinned UTM Link + Community Tab Announcement";
            alternatives.add("1x Dedicated Video + 1x Supplementary Shorts Repurpose");
        } else {
            budgetFitAssessment = "PREMIUM_TIER_EXPANDED_SCOPE";
            dataJustification = String.format(
                    "Your ৳%s BDT budget exceeds the standard single video baseline (৳%s - ৳%s BDT). This unlocks multi-platform amplification with %s, including Dedicated Feature, Shorts, and 30-day usage rights.",
                    formatCurrency(offeredBudget), formatCurrency(fairDedicatedMin), formatCurrency(fairDedicatedMax), creatorHandle
            );
            recommendedDeliverables = "1x Dedicated Deep-Dive Video + 2x Shorts + 30-Day Digital Ad Usage Rights + Pinned Comment";
            alternatives.add("Two-part episodic sponsorship series across 2 consecutive weeks");
        }

        // Try AI generation via Gemini
        AISmartPitchResponse aiResponse = null;
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                aiResponse = callGeminiForSmartPitch(creatorHandle, request, stats, budgetFitAssessment, dataJustification, recommendedDeliverables);
            } catch (Exception e) {
                log.warn("Gemini smart pitch API call failed, falling back to smart engine generator: {}", e.getMessage());
            }
        }

        if (aiResponse == null) {
            aiResponse = buildDeterministicSmartPitch(creatorHandle, request, stats, fairDedicatedMin, fairDedicatedMax, budgetFitAssessment, dataJustification, recommendedDeliverables, alternatives);
        } else {
            aiResponse.setFairMarketValueMin(fairDedicatedMin);
            aiResponse.setFairMarketValueMax(fairDedicatedMax);
            aiResponse.setBudgetFitAssessment(budgetFitAssessment);
            aiResponse.setDataJustification(dataJustification);
            aiResponse.setRecommendedDeliverables(recommendedDeliverables);
            aiResponse.setAlternativeDeliverableOptions(alternatives);
        }

        return aiResponse;
    }

    private AISmartPitchResponse buildDeterministicSmartPitch(
            String creatorHandle,
            AISmartPitchRequest req,
            CreatorProfile.PublicStats stats,
            long fairMin,
            long fairMax,
            String assessment,
            String justification,
            String deliverables,
            List<String> alternatives) {

        String brand = req.getBrandName();
        String niche = (req.getCategory() != null && !req.getCategory().isBlank()) ? req.getCategory().trim() : "Tech & Everyday Lifestyle";
        String audience = (req.getTargetAudience() != null && !req.getTargetAudience().isBlank()) ? req.getTargetAudience().trim() : "Bangladeshi consumers";
        String viewsStr = formatNumber(Math.round(stats.getAvgViews() != null ? stats.getAvgViews() : 50_000.0));
        String budgetStr = formatCurrency(req.getOfferedBudgetBDT());

        String hook = String.format(
                "Why %s x %s? %s commands strong authority in Bangladesh's %s sector with an average of %s views and high viewer sentiment. Collaborating on %s provides %s with credible third-party validation, translating creator trust directly into brand conversion.",
                creatorHandle, brand, creatorHandle, niche, viewsStr, brand, audience
        );

        String deliverablesStrategy = String.format(
                "Strategic Scope & Distribution Plan (Allocated ৳%s BDT):\n" +
                "• Recommended Deliverables: %s\n" +
                "• Brand Narrative: Real-world walkthrough illustrating how %s solves key friction points for %s.\n" +
                "• Tracking & Conversion: Prominent verbal CTA + Pinned UTM link with trackable discount/registration hook.\n" +
                "• Scope Justification: %s",
                budgetStr, deliverables, brand, audience, justification
        );

        long budgetVal = req.getOfferedBudgetBDT();
        double viewsBaseline = (stats.getAvgViews() != null && stats.getAvgViews() > 0) ? stats.getAvgViews() : 30_000.0;
        
        // Dynamic CPM calculation based on creator scale and audience niche (standard Bangladesh CPM range: 130 - 220 BDT)
        double estimatedCpmBDT = Math.max(120.0, Math.min(240.0, 150.0 + (viewsBaseline > 100_000 ? 30.0 : 0.0)));
        long calculatedImpressionsFromBudget = Math.round((budgetVal / estimatedCpmBDT) * 1000.0);
        long impressionsMin = Math.max(5_000L, Math.round(calculatedImpressionsFromBudget * 0.85));
        long impressionsMax = Math.round(calculatedImpressionsFromBudget * 1.25);

        String projectedReachStr = String.format("%s – %s Impressions", formatNumber(impressionsMin), formatNumber(impressionsMax));
        String estimatedCpmStr = String.format("৳%.0f – ৳%.0f BDT", estimatedCpmBDT * 0.9, estimatedCpmBDT * 1.1);
        String budgetAllocationRationale = String.format(
                "Your ৳%s BDT campaign budget dynamically delivers an estimated %s qualified impressions at an effective benchmark CPM of %s.",
                budgetStr, projectedReachStr, estimatedCpmStr
        );

        String roi = String.format(
                "Projected Performance & Strategic CPM Metrics:\n" +
                "• Exact Brand Budget Allocation: ৳%s BDT\n" +
                "• Projected Audience Reach: %s (%s to %s viewers in Bangladesh)\n" +
                "• Strategic Effective CPM: %s per 1,000 targeted viewers\n" +
                "• Estimated Click-Through Rate: 3.8%% – 5.5%% with trackable link\n" +
                "• Budget Allocation Rationale: %s\n" +
                "• Escrow Protection: Funds remain securely held in Johuri Escrow Vault until deliverable proof validation.",
                budgetStr, projectedReachStr, formatNumber(impressionsMin), formatNumber(impressionsMax),
                estimatedCpmStr, budgetAllocationRationale
        );

        String fullPitch = String.format(
                "### 🎯 1. Hook & Match Rationale\n%s\n\n### 📦 2. Deliverables & Data Justification\n%s\n\n### 📈 3. Projected ROI & Strategic CPM\n%s",
                hook, deliverablesStrategy, roi
        );

        String proposalEmail = String.format(
                "Subject: Partnership Opportunity: %s x %s (৳%s Campaign Proposal)\n\n" +
                "Hi %s Team,\n\n" +
                "We have reviewed your recent content benchmarks on YouTube (averaging %s views with outstanding engagement). We believe %s is a natural match for our upcoming campaign.\n\n" +
                "Here is our proposed collaboration scope:\n" +
                "- Scope: %s\n" +
                "- Proposed Budget: ৳%s BDT (Guaranteed via Johuri Escrow Vault)\n" +
                "- Estimated Reach: %s\n" +
                "- Key Messaging: Problem-solution showcase tailored to your audience.\n\n" +
                "Data Rationale:\n%s\n\n" +
                "We look forward to discussing your production timeline.\n\n" +
                "Best regards,\n" +
                "Campaign Partnerships Team, %s",
                brand, creatorHandle, budgetStr, creatorHandle, viewsStr, brand, deliverables, budgetStr, projectedReachStr, justification, brand
        );

        return AISmartPitchResponse.builder()
                .creatorHandle(creatorHandle)
                .brandName(brand)
                .offeredBudgetBDT(req.getOfferedBudgetBDT())
                .fairMarketValueMin(fairMin)
                .fairMarketValueMax(fairMax)
                .budgetFitAssessment(assessment)
                .dataJustification(justification)
                .recommendedDeliverables(deliverables)
                .alternativeDeliverableOptions(alternatives)
                .projectedReach(projectedReachStr)
                .estimatedCPM(estimatedCpmStr)
                .budgetAllocationRationale(budgetAllocationRationale)
                .hookAndMatchRationale(hook)
                .deliverablesStrategy(deliverablesStrategy)
                .projectedRoiAndCta(roi)
                .fullPitch(fullPitch)
                .proposalEmailTemplate(proposalEmail)
                .mockResponse(true)
                .build();
    }

    private CreatorProfile resolveCreatorProfile(String creatorId, String handle) {
        if (creatorId != null && !creatorId.isBlank()) {
            CreatorProfile p = creatorProfileRepository.findById(creatorId.trim()).orElse(null);
            if (p != null) return p;
        }
        if (handle != null && !handle.isBlank()) {
            String norm = handle.trim();
            if (!norm.startsWith("@")) norm = "@" + norm;
            List<CreatorProfile> list = creatorProfileRepository.findByYoutubeHandleIgnoreCase(norm);
            if (!list.isEmpty()) return list.get(0);
            List<CreatorProfile> strippedList = creatorProfileRepository.findByYoutubeHandleIgnoreCase(norm.substring(1));
            if (!strippedList.isEmpty()) return strippedList.get(0);
        }
        return null;
    }

    private CreatorProfile.PublicStats extractMetrics(AIRateEvaluationRequest req, CreatorProfile profile) {
        if (req.getMetrics() != null) {
            AIRateEvaluationRequest.MetricsPayload m = req.getMetrics();
            return CreatorProfile.PublicStats.builder()
                    .subscriberCount(m.getSubscriberCount() != null ? m.getSubscriberCount() : 100_000L)
                    .avgViews(m.getAvgViews() != null ? m.getAvgViews() : 25_000.0)
                    .engagementRate(m.getEngagementRate())
                    .audienceSentimentScore(m.getAudienceSentimentScore())
                    .build();
        }
        if (profile != null && profile.getPublicStats() != null) {
            return profile.getPublicStats();
        }
        return CreatorProfile.PublicStats.builder()
                .subscriberCount(100_000L)
                .avgViews(25_000.0)
                .engagementRate(6.5)
                .audienceSentimentScore(8.2)
                .build();
    }

    private double computeDefaultEngagement(Long subs, Double avgViews) {
        if (subs == null || subs <= 0 || avgViews == null || avgViews <= 0) return 6.8;
        double ratio = (avgViews / subs) * 100.0;
        return Math.max(3.0, Math.min(15.0, Math.round(ratio * 10.0) / 10.0));
    }

    private String callGeminiForRateAnalysis(String handle, double avgViews, double engagement, double sentiment, long fairMin, long fairMax) {
        String prompt = String.format(
                "You are an expert AI Influencer Marketing Broker in Bangladesh. Analyze Creator %s with %s average views, %.1f%% engagement, and %.1f/10 sentiment. Fair market value range is ৳%s - ৳%s BDT. Provide a concise, professional 2-3 sentence market evaluation emphasizing ROI and audience loyalty in Bangladesh.",
                handle, formatNumber(Math.round(avgViews)), engagement, sentiment, formatCurrency(fairMin), formatCurrency(fairMax)
        );

        JsonNode responseNode = callGeminiRaw(prompt);
        return extractTextFromGemini(responseNode);
    }

    private AISmartPitchResponse callGeminiForSmartPitch(
            String creatorHandle,
            AISmartPitchRequest req,
            CreatorProfile.PublicStats stats,
            String assessment,
            String justification,
            String deliverables) {

        String prompt = String.format(
                "You are an elite sponsorship strategist on Johuri.ai. Generate a high-converting sponsorship proposal in JSON format for Creator %s on behalf of Brand %s with offered budget ৳%s BDT.\n" +
                "Data metrics: Avg views=%s, Engagement=%.1f%%, Category=%s, Audience=%s.\n" +
                "Budget fit assessment: %s.\n" +
                "Data justification: %s.\n" +
                "Recommended deliverables: %s.\n\n" +
                "Output ONLY a raw JSON object with keys: hookAndMatchRationale, deliverablesStrategy, projectedRoiAndCta, proposalEmailTemplate.",
                creatorHandle, req.getBrandName(), formatCurrency(req.getOfferedBudgetBDT()),
                formatNumber(Math.round(stats.getAvgViews() != null ? stats.getAvgViews() : 50000.0)),
                stats.getEngagementRate() != null ? stats.getEngagementRate() : 7.0,
                req.getCategory(), req.getTargetAudience(), assessment, justification, deliverables
        );

        JsonNode res = callGeminiRaw(prompt);
        String text = extractTextFromGemini(res);
        if (text == null || text.isBlank()) return null;

        try {
            String jsonClean = cleanJsonMarkdown(text);
            JsonNode root = objectMapper.readTree(jsonClean);
            String hook = root.path("hookAndMatchRationale").asText("");
            String deliv = root.path("deliverablesStrategy").asText("");
            String roi = root.path("projectedRoiAndCta").asText("");
            String email = root.path("proposalEmailTemplate").asText("");

            String full = String.format("### 🎯 1. Hook & Match Rationale\n%s\n\n### 📦 2. Deliverables & Data Strategy\n%s\n\n### 📈 3. Projected ROI\n%s", hook, deliv, roi);

            return AISmartPitchResponse.builder()
                    .creatorHandle(creatorHandle)
                    .brandName(req.getBrandName())
                    .offeredBudgetBDT(req.getOfferedBudgetBDT())
                    .hookAndMatchRationale(hook)
                    .deliverablesStrategy(deliv)
                    .projectedRoiAndCta(roi)
                    .fullPitch(full)
                    .proposalEmailTemplate(email)
                    .mockResponse(false)
                    .build();
        } catch (Exception e) {
            log.warn("Could not parse JSON from Gemini smart pitch: {}", e.getMessage());
            return null;
        }
    }

    private JsonNode callGeminiRaw(String userPrompt) {
        URI uri = URI.create(String.format(GEMINI_API_URL, model) + "?key=" + apiKey);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", userPrompt)
                        ))
                )
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        String raw = restTemplate.postForObject(uri, requestEntity, String.class);
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractTextFromGemini(JsonNode responseNode) {
        if (responseNode == null) return null;
        try {
            return responseNode
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            return null;
        }
    }

    private String cleanJsonMarkdown(String raw) {
        String clean = raw.trim();
        if (clean.startsWith("```json")) {
            clean = clean.substring(7);
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        return clean.trim();
    }

    private String formatNumber(long num) {
        if (num >= 1_000_000) {
            double val = num / 1_000_000.0;
            return (val == (long) val) ? String.format("%dM", (long) val) : String.format("%.1fM", val);
        }
        if (num >= 1_000) {
            double val = num / 1_000.0;
            return (val == (long) val) ? String.format("%dK", (long) val) : String.format("%.1fK", val);
        }
        return String.valueOf(num);
    }

    private String formatCurrency(long val) {
        return String.format("%,d", val);
    }
}

