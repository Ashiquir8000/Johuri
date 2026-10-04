package com.example.johuri.controller;

import com.example.johuri.DTO.ApiResponse;
import com.example.johuri.DTO.CreatorProfileClaimRequest;
import com.example.johuri.DTO.CreatorProfileClaimResponse;
import com.example.johuri.DTO.MediaKitPublicResponse;
import com.example.johuri.service.CreatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/creators")
public class CreatorController {

    private final CreatorService creatorService;

    @GetMapping("/public/{handle}")
    public ResponseEntity<ApiResponse<MediaKitPublicResponse>> getPublicChannelTelemetry(
            @PathVariable String handle) {
        return ResponseEntity.ok(ApiResponse.success(
                "Public channel telemetry retrieved",
                creatorService.getPublicChannelTelemetry(handle)
        ));
    }

    @PostMapping("/claim")
    public ResponseEntity<ApiResponse<CreatorProfileClaimResponse>> claimProfile(
            @Valid @RequestBody CreatorProfileClaimRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Creator profile claimed",
                creatorService.claimProfile(request)
        ));
    }
}
