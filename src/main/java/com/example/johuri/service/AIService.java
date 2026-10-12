package com.example.johuri.service;

import com.example.johuri.DTO.AIRateEvaluationRequest;
import com.example.johuri.DTO.AIRateEvaluationResponse;
import com.example.johuri.DTO.AISmartPitchRequest;
import com.example.johuri.DTO.AISmartPitchResponse;

public interface AIService {

    AIRateEvaluationResponse evaluateRate(AIRateEvaluationRequest request);

    AISmartPitchResponse generateSmartPitch(AISmartPitchRequest request);
}

