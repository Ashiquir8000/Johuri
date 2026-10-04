package com.example.johuri.controller;

import com.example.johuri.DTO.AIEmailEvaluationRequest;
import com.example.johuri.DTO.ApiResponse;
import com.example.johuri.DTO.GeminiAIResponse;
import com.example.johuri.entity.CreatorProfile;
import com.example.johuri.service.GeminiAIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AIController {

    private final GeminiAIService geminiAIService;

    @PostMapping("/evaluate-rate")
    public ResponseEntity<ApiResponse<GeminiAIResponse>> evaluateRate(
            @Valid @RequestBody AIEmailEvaluationRequest request) {
        AIEmailEvaluationRequest.CreatorMetrics metrics = request.getCreatorMetrics();
        CreatorProfile.PublicStats creatorStats = CreatorProfile.PublicStats.builder()
                .subscriberCount(metrics.getSubscriberCount())
                .avgViews(metrics.getAvgViews())
                .build();

        return ResponseEntity.ok(ApiResponse.success(
                "Sponsorship rate evaluated",
                geminiAIService.analyzeBrandEmail(request.getBrandEmailBody(), creatorStats)
        ));
    }
}
