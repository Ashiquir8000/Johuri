package com.example.johuri.service;

import com.example.johuri.DTO.DealCreateRequest;
import com.example.johuri.DTO.DealResponse;
import com.example.johuri.entity.Deal;
import com.example.johuri.repository.DealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DealService {

    private final DealRepository dealRepository;

    public DealResponse createDeal(DealCreateRequest request) {
        Deal deal = Deal.builder()
                .creatorId(request.getCreatorId().trim())
                .brandName(request.getBrandName().trim())
                .brandEmail(request.getBrandEmail().trim())
                .agreedAmountBDT(request.getAgreedAmountBDT())
                .status(Deal.Status.IN_DISCUSSION)
                .createdAt(Instant.now())
                .build();

        return toResponse(dealRepository.save(deal));
    }

    public List<DealResponse> getDealsForCreator(String creatorId) {
        if (creatorId == null || creatorId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Creator ID must not be blank");
        }
        return dealRepository.findByCreatorId(creatorId.trim()).stream()
                .map(this::toResponse)
                .toList();
    }

    public DealResponse updateStatus(String dealId, Deal.Status requestedStatus) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deal was not found"));
        if (requestedStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deal status must not be null");
        }
        if (deal.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deal has no current status");
        }
        if (requestedStatus == Deal.Status.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A deal can only be marked PAID after payment confirmation"
            );
        }
        if (requestedStatus == deal.getStatus()) {
            return toResponse(deal);
        }

        int currentIndex = deal.getStatus().ordinal();
        int requestedIndex = requestedStatus.ordinal();
        if (requestedIndex != currentIndex + 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Deal status must advance one step at a time through the pipeline"
            );
        }

        deal.setStatus(requestedStatus);
        return toResponse(dealRepository.save(deal));
    }

    private DealResponse toResponse(Deal deal) {
        return DealResponse.builder()
                .id(deal.getId())
                .creatorId(deal.getCreatorId())
                .brandName(deal.getBrandName())
                .brandEmail(deal.getBrandEmail())
                .agreedAmountBDT(deal.getAgreedAmountBDT())
                .status(deal.getStatus())
                .invoicePdfUrl(deal.getInvoicePdfUrl())
                .createdAt(deal.getCreatedAt())
                .build();
    }
}
