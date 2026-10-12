package com.example.johuri.service;

import com.example.johuri.DTO.EscrowConfirmRequest;
import com.example.johuri.DTO.EscrowConfirmResponse;
import com.example.johuri.DTO.EscrowInitiateRequest;
import com.example.johuri.DTO.EscrowInitiateResponse;
import com.example.johuri.entity.CampaignDeal;
import com.example.johuri.repository.CampaignDealRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EscrowServiceTests {

    private CampaignDealRepository repository;
    private com.example.johuri.repository.UserRepository userRepository;
    private EscrowService escrowService;

    @BeforeEach
    void setUp() {
        repository = mock(CampaignDealRepository.class);
        userRepository = mock(com.example.johuri.repository.UserRepository.class);
        escrowService = new EscrowService(repository, userRepository);
    }

    @Test
    void initiatesEscrowWithPendingStatusAndSession() {
        EscrowInitiateRequest request = EscrowInitiateRequest.builder()
                .brandName("Pathao")
                .creatorHandle("@khalidfarhan")
                .budgetBDT(150_000L)
                .build();

        EscrowInitiateResponse response = escrowService.initiateEscrow(request);

        assertNotNull(response.getDealId());
        assertNotNull(response.getBkashSessionId());
        assertEquals("PENDING", response.getStatus());
        assertEquals(150_000L, response.getAmountBDT());
        assertEquals("Pathao", response.getBrandName());
        assertEquals("@khalidfarhan", response.getCreatorHandle());
        assertTrue(response.getMerchantNumber().contains("JOHURI"));

        ArgumentCaptor<CampaignDeal> captor = ArgumentCaptor.forClass(CampaignDeal.class);
        verify(repository).save(captor.capture());
        assertEquals(CampaignDeal.Status.PENDING, captor.getValue().getStatus());
    }

    @Test
    void confirmsPaymentAndLocksEscrow() {
        CampaignDeal pendingDeal = CampaignDeal.builder()
                .dealId("JD-12345678")
                .bkashSessionId("BKASH-SESS-ABC")
                .brandName("Chaldal")
                .creatorHandle("@NadirOnTheGo")
                .budgetBDT(200_000L)
                .status(CampaignDeal.Status.PENDING)
                .build();

        when(repository.findByDealId("JD-12345678")).thenReturn(Optional.of(pendingDeal));
        when(repository.save(any(CampaignDeal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EscrowConfirmRequest confirmRequest = EscrowConfirmRequest.builder()
                .dealId("JD-12345678")
                .bkashSessionId("BKASH-SESS-ABC")
                .otp("123456")
                .pin("12345")
                .build();

        EscrowConfirmResponse response = escrowService.confirmPayment(confirmRequest);

        assertEquals("ESCROW_LOCKED", response.getStatus());
        assertEquals("JD-12345678", response.getDealId());
        assertNotNull(response.getTransactionId());
        assertTrue(response.getMessage().contains("Securely Locked in Johuri Escrow"));
        assertEquals(CampaignDeal.Status.ESCROW_LOCKED, pendingDeal.getStatus());
    }

    @Test
    void releasesFundsSuccessfully() {
        CampaignDeal lockedDeal = CampaignDeal.builder()
                .dealId("JD-88889999")
                .brandName("Aarong")
                .creatorHandle("@RafsanTheChotoBhai")
                .budgetBDT(50_000L)
                .status(CampaignDeal.Status.ESCROW_LOCKED)
                .build();

        when(repository.findByDealId("JD-88889999")).thenReturn(Optional.of(lockedDeal));
        when(repository.save(any(CampaignDeal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CampaignDeal result = escrowService.releaseFunds("JD-88889999");

        assertEquals(CampaignDeal.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());
        verify(repository).save(lockedDeal);
    }

    @Test
    void submitsProofLinkSuccessfully() {
        CampaignDeal deal = CampaignDeal.builder()
                .dealId("JD-7777")
                .status(CampaignDeal.Status.ESCROW_LOCKED)
                .build();

        when(repository.findByDealId("JD-7777")).thenReturn(Optional.of(deal));
        when(repository.save(any(CampaignDeal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CampaignDeal result = escrowService.submitProof("JD-7777", "https://youtu.be/sample123");

        assertEquals("https://youtu.be/sample123", result.getProofLink());
        verify(repository).save(deal);
    }
}

