package com.example.johuri.controller;

import com.example.johuri.DTO.ApiResponse;
import com.example.johuri.DTO.DealCreateRequest;
import com.example.johuri.DTO.DealResponse;
import com.example.johuri.DTO.DealStatusUpdateRequest;
import com.example.johuri.service.DealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deals")
public class DealController {

    private final DealService dealService;

    @PostMapping
    public ResponseEntity<ApiResponse<DealResponse>> createDeal(
            @Valid @RequestBody DealCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Deal created", dealService.createDeal(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DealResponse>>> getDealsForCreator(
            @RequestParam String creatorId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Deals retrieved",
                dealService.getDealsForCreator(creatorId)
        ));
    }

    @PatchMapping("/{dealId}/status")
    public ResponseEntity<ApiResponse<DealResponse>> updateStatus(
            @PathVariable String dealId,
            @Valid @RequestBody DealStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Deal status updated",
                dealService.updateStatus(dealId, request.getStatus())
        ));
    }
}
