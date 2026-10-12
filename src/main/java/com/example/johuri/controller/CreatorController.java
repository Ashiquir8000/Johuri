package com.example.johuri.controller;

import com.example.johuri.DTO.AIPitchRequest;
import com.example.johuri.DTO.AIPitchResponse;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/creators")
public class CreatorController {

    private final CreatorService creatorService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MediaKitPublicResponse>>> getCreators() {
        return ResponseEntity.ok(ApiResponse.success(
                "Creators retrieved",
                creatorService.getCreators()
        ));
    }

    @GetMapping("/{handle}")
    public ResponseEntity<ApiResponse<MediaKitPublicResponse>> getCreatorByHandle(
            @PathVariable String handle) {
        String decodedHandle = java.net.URLDecoder.decode(handle, java.nio.charset.StandardCharsets.UTF_8).trim();
        return ResponseEntity.ok(ApiResponse.success(
                "Creator profile retrieved",
                creatorService.getCreatorByHandle(decodedHandle)
        ));
    }

    @GetMapping("/public/{handle}")
    public ResponseEntity<ApiResponse<MediaKitPublicResponse>> getPublicChannelTelemetry(
            @PathVariable String handle) {
        String decodedHandle = java.net.URLDecoder.decode(handle, java.nio.charset.StandardCharsets.UTF_8).trim();
        return ResponseEntity.ok(ApiResponse.success(
                "Public channel telemetry retrieved",
                creatorService.getPublicChannelTelemetry(decodedHandle)
        ));
    }

    @RequestMapping(value = "/sync/{handle}", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
    public ResponseEntity<ApiResponse<com.example.johuri.DTO.YouTubeTelemetryDTO>> syncCreatorTelemetry(
            @PathVariable String handle) {
        String decodedHandle = java.net.URLDecoder.decode(handle, java.nio.charset.StandardCharsets.UTF_8);
        if (decodedHandle.startsWith("@")) {
            decodedHandle = decodedHandle.substring(1);
        }
        return ResponseEntity.ok(ApiResponse.success(
                "Creator telemetry synced",
                creatorService.syncCreatorTelemetry(decodedHandle)
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

    @PostMapping("/{handle}/generate-pitch")
    public ResponseEntity<ApiResponse<AIPitchResponse>> generatePitch(
            @PathVariable String handle,
            @Valid @RequestBody AIPitchRequest request) {
        String decodedHandle = java.net.URLDecoder.decode(handle, java.nio.charset.StandardCharsets.UTF_8).trim();
        return ResponseEntity.ok(ApiResponse.success(
                "AI Pitch generated successfully",
                creatorService.generatePitch(decodedHandle, request)
        ));
    }
}
