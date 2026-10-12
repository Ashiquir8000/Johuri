package com.example.johuri.service;

import com.example.johuri.DTO.EscrowConfirmRequest;
import com.example.johuri.DTO.EscrowConfirmResponse;
import com.example.johuri.DTO.EscrowInitiateRequest;
import com.example.johuri.DTO.EscrowInitiateResponse;
import com.example.johuri.entity.CampaignDeal;
import com.example.johuri.entity.User;
import com.example.johuri.repository.CampaignDealRepository;
import com.example.johuri.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscrowService {

    private final CampaignDealRepository campaignDealRepository;
    private final UserRepository userRepository;
    private static final String DUMMY_MERCHANT_NUMBER = "01700-JOHURI (01700-564874)";

    public EscrowInitiateResponse initiateEscrow(EscrowInitiateRequest request) {
        String dealId = "JD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String sessionId = "BKASH-SESS-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        CampaignDeal deal = CampaignDeal.builder()
                .dealId(dealId)
                .brandName(request.getBrandName().trim())
                .creatorHandle(request.getCreatorHandle().trim())
                .budgetBDT(request.getBudgetBDT())
                .deliverableType(request.getDeliverableType() != null && !request.getDeliverableType().isBlank()
                        ? request.getDeliverableType().trim() : "Custom Sponsorship & Integration")
                .status(CampaignDeal.Status.PENDING)
                .bkashSessionId(sessionId)
                .createdAt(Instant.now())
                .build();

        campaignDealRepository.save(deal);
        log.info("Initiated campaign deal {} for brand {} and creator {} with budget ৳{}",
                dealId, request.getBrandName(), request.getCreatorHandle(), request.getBudgetBDT());

        return EscrowInitiateResponse.builder()
                .dealId(dealId)
                .bkashSessionId(sessionId)
                .merchantNumber(DUMMY_MERCHANT_NUMBER)
                .amountBDT(request.getBudgetBDT())
                .brandName(request.getBrandName())
                .creatorHandle(request.getCreatorHandle())
                .status(deal.getStatus().name())
                .build();
    }

    public EscrowConfirmResponse confirmPayment(EscrowConfirmRequest request) {
        CampaignDeal deal = campaignDealRepository.findByDealId(request.getDealId().trim())
                .orElseGet(() -> {
                    if (request.getBkashSessionId() != null && !request.getBkashSessionId().isBlank()) {
                        return campaignDealRepository.findByBkashSessionId(request.getBkashSessionId().trim())
                                .orElse(null);
                    }
                    return null;
                });

        if (deal == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign deal not found for ID: " + request.getDealId());
        }

        String transactionId = "TRX" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        deal.setStatus(CampaignDeal.Status.ESCROW_LOCKED);
        deal.setTransactionId(transactionId);
        campaignDealRepository.save(deal);

        log.info("Deal {} successfully locked in escrow with transaction ID {}", deal.getDealId(), transactionId);

        return EscrowConfirmResponse.builder()
                .dealId(deal.getDealId())
                .status(deal.getStatus().name())
                .amountBDT(deal.getBudgetBDT())
                .transactionId(transactionId)
                .creatorHandle(deal.getCreatorHandle())
                .brandName(deal.getBrandName())
                .message("৳" + deal.getBudgetBDT() + " Securely Locked in Johuri Escrow! Campaign initialized with " + deal.getCreatorHandle())
                .lockedAt(Instant.now())
                .build();
    }

    public java.util.List<com.example.johuri.entity.CampaignDeal> getAllDeals(String creatorHandle, String brandName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String role = null;
            String creatorHandleClaim = null;

            if (auth.getDetails() instanceof io.jsonwebtoken.Claims claims) {
                role = (String) claims.get("role");
                creatorHandleClaim = (String) claims.get("creatorHandle");
            }

            if (role == null && auth.getAuthorities() != null) {
                for (GrantedAuthority ga : auth.getAuthorities()) {
                    if (ga.getAuthority().startsWith("ROLE_")) {
                        role = ga.getAuthority().substring(5);
                        break;
                    }
                }
            }

            if ("CREATOR".equalsIgnoreCase(role)) {
                String handle = (creatorHandleClaim != null && !creatorHandleClaim.isBlank())
                        ? creatorHandleClaim : creatorHandle;
                if (handle != null && !handle.isBlank()) {
                    String clean = handle.trim();
                    if (!clean.startsWith("@")) clean = "@" + clean;
                    return campaignDealRepository.findByCreatorHandleIgnoreCase(clean);
                }
            } else if ("BRAND".equalsIgnoreCase(role)) {
                String effectiveBrand = brandName;
                if (effectiveBrand == null || effectiveBrand.isBlank()) {
                    String email = auth.getName();
                    User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
                    if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) {
                        effectiveBrand = user.getFullName();
                    } else {
                        effectiveBrand = email;
                    }
                }
                return campaignDealRepository.findByBrandNameIgnoreCase(effectiveBrand.trim());
            } else if ("ADMIN".equalsIgnoreCase(role)) {
                if (creatorHandle != null && !creatorHandle.isBlank()) {
                    String clean = creatorHandle.trim();
                    if (!clean.startsWith("@")) clean = "@" + clean;
                    return campaignDealRepository.findByCreatorHandleIgnoreCase(clean);
                }
                if (brandName != null && !brandName.isBlank()) {
                    return campaignDealRepository.findByBrandNameIgnoreCase(brandName.trim());
                }
                return campaignDealRepository.findAll();
            }
        }

        // Fallback for unauthenticated/demo calls or query parameter filtering
        if (creatorHandle != null && !creatorHandle.isBlank()) {
            String clean = creatorHandle.trim();
            if (!clean.startsWith("@")) clean = "@" + clean;
            return campaignDealRepository.findByCreatorHandleIgnoreCase(clean);
        }
        if (brandName != null && !brandName.isBlank()) {
            return campaignDealRepository.findByBrandNameIgnoreCase(brandName.trim());
        }
        return campaignDealRepository.findAll();
    }

    public CampaignDeal releaseFunds(String dealId) {
        CampaignDeal deal = campaignDealRepository.findByDealId(dealId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deal not found: " + dealId));

        deal.setStatus(CampaignDeal.Status.COMPLETED);
        deal.setCompletedAt(Instant.now());
        campaignDealRepository.save(deal);

        log.info("Funds released for Deal {}, status updated to COMPLETED at {}", dealId, deal.getCompletedAt());
        return deal;
    }

    public CampaignDeal submitProof(String dealId, String proofLink) {
        CampaignDeal deal = campaignDealRepository.findByDealId(dealId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deal not found: " + dealId));

        deal.setProofLink(proofLink != null ? proofLink.trim() : null);
        campaignDealRepository.save(deal);

        log.info("Proof link submitted for Deal {}: {}", dealId, proofLink);
        return deal;
    }
}

