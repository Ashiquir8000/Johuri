package com.example.johuri.controller;

import com.example.johuri.DTO.AIRateEvaluationRequest;
import com.example.johuri.DTO.AIRateEvaluationResponse;
import com.example.johuri.DTO.AISmartPitchRequest;
import com.example.johuri.DTO.AISmartPitchResponse;
import com.example.johuri.DTO.ApiResponse;
import com.example.johuri.service.AIService;
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

    private final AIService aiService;

    @PostMapping("/evaluate-rate")
    public ResponseEntity<ApiResponse<AIRateEvaluationResponse>> evaluateRate(
            @Valid @RequestBody AIRateEvaluationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Creator performance and dynamic rate evaluation completed successfully",
                aiService.evaluateRate(request)
        ));
    }

    @PostMapping("/smart-pitch")
    public ResponseEntity<ApiResponse<AISmartPitchResponse>> generateSmartPitch(
            @Valid @RequestBody AISmartPitchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Strategic data-backed pitch proposal generated successfully",
                aiService.generateSmartPitch(request)
        ));
    }
}
