package com.example.johuri.controller;

import com.example.johuri.DTO.ApiResponse;
import com.example.johuri.DTO.EscrowConfirmRequest;
import com.example.johuri.DTO.EscrowConfirmResponse;
import com.example.johuri.DTO.EscrowInitiateRequest;
import com.example.johuri.DTO.EscrowInitiateResponse;
import com.example.johuri.service.EscrowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/escrow")
public class EscrowPaymentController {

    private final EscrowService escrowService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<EscrowInitiateResponse>> initiateEscrow(
            @Valid @RequestBody EscrowInitiateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Escrow session initiated",
                escrowService.initiateEscrow(request)
        ));
    }

    @PostMapping("/confirm-payment")
    public ResponseEntity<ApiResponse<EscrowConfirmResponse>> confirmPayment(
            @Valid @RequestBody EscrowConfirmRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Escrow payment confirmed and locked",
                escrowService.confirmPayment(request)
        ));
    }

    @org.springframework.web.bind.annotation.GetMapping("/deals")
    public ResponseEntity<ApiResponse<java.util.List<com.example.johuri.entity.CampaignDeal>>> getDeals(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String creatorHandle,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String brandName) {
        return ResponseEntity.ok(ApiResponse.success(
                "Campaign deals retrieved successfully",
                escrowService.getAllDeals(creatorHandle, brandName)
        ));
    }

    @PostMapping("/release-funds")
    public ResponseEntity<ApiResponse<com.example.johuri.entity.CampaignDeal>> releaseFunds(
            @Valid @RequestBody com.example.johuri.DTO.ReleaseFundsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Escrow funds released to creator successfully",
                escrowService.releaseFunds(request.getDealId())
        ));
    }

    @PostMapping("/submit-proof")
    public ResponseEntity<ApiResponse<com.example.johuri.entity.CampaignDeal>> submitProof(
            @Valid @RequestBody com.example.johuri.DTO.SubmitProofRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Deliverable proof link submitted successfully",
                escrowService.submitProof(request.getDealId(), request.getProofLink())
        ));
    }
}

