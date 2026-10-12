package com.example.johuri.service;

import com.example.johuri.DTO.AdminOverviewResponse;
import com.example.johuri.entity.CampaignDeal;
import com.example.johuri.repository.CampaignDealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final CampaignDealRepository campaignDealRepository;

    public AdminOverviewResponse getOverview() {
        List<CampaignDeal> allDeals = campaignDealRepository.findAll();

        long totalEscrowLockedBDT = 0L;
        long completedVolumeBDT = 0L;
        long disputedDealsCount = 0L;
        long activeCampaignsCount = 0L;

        for (CampaignDeal deal : allDeals) {
            long budget = deal.getBudgetBDT() != null ? deal.getBudgetBDT() : 0L;
            CampaignDeal.Status status = deal.getStatus();

            if (status == CampaignDeal.Status.ESCROW_LOCKED ||
                status == CampaignDeal.Status.IN_ESCROW ||
                status == CampaignDeal.Status.DELIVERABLE_SUBMITTED ||
                status == CampaignDeal.Status.DISPUTED) {
                totalEscrowLockedBDT += budget;
                activeCampaignsCount++;
            }

            if (status == CampaignDeal.Status.COMPLETED ||
                status == CampaignDeal.Status.FORCE_RELEASED) {
                completedVolumeBDT += budget;
            }

            if (status == CampaignDeal.Status.DISPUTED) {
                disputedDealsCount++;
            }
        }

        double platformCommissionBDT = completedVolumeBDT * 0.10; // 10% platform commission

        return AdminOverviewResponse.builder()
                .totalEscrowLockedBDT(totalEscrowLockedBDT)
                .completedVolumeBDT(completedVolumeBDT)
                .disputedDealsCount(disputedDealsCount)
                .platformCommissionBDT(platformCommissionBDT)
                .activeCampaignsCount(activeCampaignsCount)
                .totalDealsCount((long) allDeals.size())
                .build();
    }

    public List<CampaignDeal> getAllDeals(String statusFilter) {
        List<CampaignDeal> allDeals = campaignDealRepository.findAll();

        if (statusFilter != null && !statusFilter.isBlank() && !statusFilter.equalsIgnoreCase("ALL")) {
            String filter = statusFilter.trim().toUpperCase();
            allDeals = allDeals.stream()
                    .filter(d -> {
                        if (d.getStatus() == null) return false;
                        String statusName = d.getStatus().name();
                        if (filter.equals("IN_ESCROW")) {
                            return statusName.equals("IN_ESCROW") || statusName.equals("ESCROW_LOCKED");
                        }
                        return statusName.equalsIgnoreCase(filter);
                    })
                    .collect(Collectors.toList());
        }

        allDeals.sort(Comparator.comparing(
                (CampaignDeal d) -> d.getCreatedAt() != null ? d.getCreatedAt() : Instant.EPOCH
        ).reversed());

        return allDeals;
    }

    public CampaignDeal forceRelease(String dealId, String adminNotes, String adminEmail) {
        CampaignDeal deal = findDealOrThrow(dealId);
        validateActionableStatus(deal);

        deal.setStatus(CampaignDeal.Status.FORCE_RELEASED);
        deal.setCompletedAt(Instant.now());
        deal.setResolvedAt(Instant.now());
        deal.setResolvedBy(adminEmail != null ? adminEmail : "admin@johuri.ai");
        deal.setAdminNotes(adminNotes != null ? adminNotes.trim() : "Force released by Admin");

        appendAudit(deal, "FORCE_RELEASED", adminEmail, adminNotes);
        campaignDealRepository.save(deal);

        log.info("[ADMIN AUDIT] Deal {} force-released by {} with notes: {}", dealId, adminEmail, adminNotes);
        return deal;
    }

    public CampaignDeal refund(String dealId, String adminNotes, String adminEmail) {
        CampaignDeal deal = findDealOrThrow(dealId);
        validateActionableStatus(deal);

        deal.setStatus(CampaignDeal.Status.REFUNDED);
        deal.setResolvedAt(Instant.now());
        deal.setResolvedBy(adminEmail != null ? adminEmail : "admin@johuri.ai");
        deal.setAdminNotes(adminNotes != null ? adminNotes.trim() : "Refunded to Brand by Admin");

        appendAudit(deal, "REFUNDED", adminEmail, adminNotes);
        campaignDealRepository.save(deal);

        log.info("[ADMIN AUDIT] Deal {} refunded to brand by {} with notes: {}", dealId, adminEmail, adminNotes);
        return deal;
    }

    public CampaignDeal reviewDeliverable(String dealId, String action, String adminNotes, String adminEmail) {
        CampaignDeal deal = findDealOrThrow(dealId);

        String act = action != null ? action.trim().toUpperCase() : "APPROVE";
        if (!act.equals("APPROVE") && !act.equals("REJECT")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review action must be APPROVE or REJECT");
        }

        if (act.equals("APPROVE")) {
            deal.setDeliverableReviewStatus("APPROVED");
            deal.setStatus(CampaignDeal.Status.COMPLETED);
            deal.setCompletedAt(Instant.now());
        } else {
            deal.setDeliverableReviewStatus("REJECTED");
            deal.setStatus(CampaignDeal.Status.DISPUTED);
        }

        deal.setResolvedAt(Instant.now());
        deal.setResolvedBy(adminEmail != null ? adminEmail : "admin@johuri.ai");
        deal.setAdminNotes(adminNotes != null ? adminNotes.trim() : "Reviewed by Admin");

        appendAudit(deal, "REVIEW_DELIVERABLE_" + act, adminEmail, adminNotes);
        campaignDealRepository.save(deal);

        log.info("[ADMIN AUDIT] Deal {} deliverable reviewed ({}) by {} with notes: {}", dealId, act, adminEmail, adminNotes);
        return deal;
    }

    private CampaignDeal findDealOrThrow(String dealId) {
        return campaignDealRepository.findByDealId(dealId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign deal not found: " + dealId));
    }

    private void validateActionableStatus(CampaignDeal deal) {
        CampaignDeal.Status s = deal.getStatus();
        if (s == CampaignDeal.Status.COMPLETED ||
            s == CampaignDeal.Status.FORCE_RELEASED ||
            s == CampaignDeal.Status.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot execute financial override. Deal is already finalized with status: " + s);
        }
    }

    private void appendAudit(CampaignDeal deal, String action, String performedBy, String notes) {
        if (deal.getAuditTrail() == null) {
            deal.setAuditTrail(new ArrayList<>());
        }
        deal.getAuditTrail().add(CampaignDeal.AuditEntry.builder()
                .action(action)
                .performedBy(performedBy != null ? performedBy : "admin@johuri.ai")
                .notes(notes)
                .timestamp(Instant.now())
                .build());
    }
}

