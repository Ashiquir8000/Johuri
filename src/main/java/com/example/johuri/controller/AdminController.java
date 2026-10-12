package com.example.johuri.controller;

import com.example.johuri.DTO.AdminActionRequest;
import com.example.johuri.DTO.AdminOverviewResponse;
import com.example.johuri.DTO.ApiResponse;
import com.example.johuri.DTO.ReviewDeliverableRequest;
import com.example.johuri.entity.CampaignDeal;
import com.example.johuri.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/deals/overview")
    public ResponseEntity<ApiResponse<AdminOverviewResponse>> getDealsOverview() {
        return ResponseEntity.ok(ApiResponse.success(
                "Admin deals overview retrieved",
                adminService.getOverview()
        ));
    }

    @GetMapping("/deals")
    public ResponseEntity<ApiResponse<List<CampaignDeal>>> getDeals(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(
                "Campaign deals retrieved successfully",
                adminService.getAllDeals(status)
        ));
    }

    @PostMapping("/escrow/{dealId}/force-release")
    public ResponseEntity<ApiResponse<CampaignDeal>> forceRelease(
            @PathVariable String dealId,
            @Valid @RequestBody AdminActionRequest request,
            Authentication authentication) {
        String adminEmail = authentication != null ? authentication.getName() : "admin@johuri.ai";
        CampaignDeal deal = adminService.forceRelease(dealId, request.getAdminNotes(), adminEmail);
        return ResponseEntity.ok(ApiResponse.success(
                "Escrow funds force-released to creator with admin audit logged",
                deal
        ));
    }

    @PostMapping("/escrow/{dealId}/refund")
    public ResponseEntity<ApiResponse<CampaignDeal>> refundToBrand(
            @PathVariable String dealId,
            @Valid @RequestBody AdminActionRequest request,
            Authentication authentication) {
        String adminEmail = authentication != null ? authentication.getName() : "admin@johuri.ai";
        CampaignDeal deal = adminService.refund(dealId, request.getAdminNotes(), adminEmail);
        return ResponseEntity.ok(ApiResponse.success(
                "Escrow funds refunded to brand with admin audit logged",
                deal
        ));
    }

    @PostMapping("/deals/{dealId}/review-deliverable")
    public ResponseEntity<ApiResponse<CampaignDeal>> reviewDeliverable(
            @PathVariable String dealId,
            @Valid @RequestBody ReviewDeliverableRequest request,
            Authentication authentication) {
        String adminEmail = authentication != null ? authentication.getName() : "admin@johuri.ai";
        CampaignDeal deal = adminService.reviewDeliverable(
                dealId,
                request.getAction(),
                request.getAdminNotes(),
                adminEmail
        );
        return ResponseEntity.ok(ApiResponse.success(
                "Deliverable review completed successfully",
                deal
        ));
    }
}

