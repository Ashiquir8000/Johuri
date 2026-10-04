package com.example.johuri.service;

import com.example.johuri.DTO.GeminiAIResponse;
import com.example.johuri.entity.CreatorProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiAIServiceTests {

    private final GeminiAIService service = new GeminiAIService(
            "",
            "gemini-2.0-flash",
            new RestTemplate(),
            new ObjectMapper()
    );

    @Test
    void returnsStructuredMockAnalysisWhenApiKeyIsMissing() {
        CreatorProfile.PublicStats metrics = CreatorProfile.PublicStats.builder()
                .subscriberCount(25_000L)
                .avgViews(12_200.0)
                .build();

        GeminiAIResponse response = service.analyzeBrandEmail("We would like to sponsor a video.", metrics);

        assertEquals(12_500L, response.getFairValuationBDT());
        assertTrue(response.isMockResponse());
        assertTrue(response.getCounterProposalText().contains("BDT 12500"));
        assertTrue(response.getCounterProposalText().contains("mock estimate"));
    }

    @Test
    void rejectsMissingCreatorMetrics() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.analyzeBrandEmail("We would like to sponsor a video.", null)
        );
    }
}
