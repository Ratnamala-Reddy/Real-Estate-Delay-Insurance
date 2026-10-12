package org.example.claimservice.controller;

import org.example.claimservice.dto.CreateClaimRequest;
import org.example.claimservice.entity.Claim;
import org.example.claimservice.enums.ClaimStatus;
import org.example.claimservice.exceptions.ClaimNotFoundException;
import org.example.claimservice.security.AuthenticatedUser;
import org.example.claimservice.service.ClaimService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimControllerTest {

    @Mock
    private ClaimService claimService;

    private ClaimController claimController;

    private AuthenticatedUser buyer;

    private AuthenticatedUser admin;

    private static final String CLAIM_ID =
            "claim-101";

    private static final String POLICY_ID =
            "policy-101";

    private static final String APPLICATION_ID =
            "application-101";

    private static final Integer BUYER_USER_ID =
            4;

    @BeforeEach
    void setUp() {

        claimController =
                new ClaimController(
                        claimService
                );

        buyer =
                new AuthenticatedUser(
                        BUYER_USER_ID,
                        "buyer@gmail.com",
                        "BUYER"
                );

        admin =
                new AuthenticatedUser(
                        1,
                        "admin@gmail.com",
                        "ADMIN"
                );
    }

    // =========================================================
    // CREATE CLAIM
    // =========================================================

    @Test
    void createClaim_ShouldReturnSubmittedClaim() {

        CreateClaimRequest request =
                createClaimRequest();

        Claim claim =
                createClaim(
                        ClaimStatus.DOCUMENTS_SUBMITTED.name()
                );

        when(claimService.createClaim(
                request,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimController.createClaim(
                                request,
                                buyer
                        )
                )
                .assertNext(response -> {

                    assertEquals(
                            CLAIM_ID,
                            response.getClaimId()
                    );

                    assertEquals(
                            POLICY_ID,
                            response.getPolicyId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            response.getApplicationId()
                    );

                    assertEquals(
                            500000.0,
                            response.getClaimedAmount()
                    );

                    assertEquals(
                            ClaimStatus.DOCUMENTS_SUBMITTED.name(),
                            response.getStatus()
                    );

                    assertEquals(
                            "Claim submitted for review",
                            response.getRemarks()
                    );
                })
                .verifyComplete();

        verify(claimService)
                .createClaim(
                        request,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    @Test
    void createClaim_ShouldPassAuthenticatedBuyerIdentity() {

        CreateClaimRequest request =
                createClaimRequest();

        Claim claim =
                createClaim(
                        ClaimStatus.DOCUMENTS_SUBMITTED.name()
                );

        when(claimService.createClaim(
                request,
                4,
                "BUYER"
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimController.createClaim(
                                request,
                                buyer
                        )
                )
                .expectNext(claim)
                .verifyComplete();

        verify(claimService)
                .createClaim(
                        request,
                        4,
                        "BUYER"
                );
    }

    @Test
    void createClaim_ShouldPropagateServiceError() {

        CreateClaimRequest request =
                createClaimRequest();

        RuntimeException exception =
                new RuntimeException(
                        "Claim creation failed"
                );

        when(claimService.createClaim(
                request,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        claimController.createClaim(
                                request,
                                buyer
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && "Claim creation failed"
                                .equals(error.getMessage())
                )
                .verify();

        verify(claimService)
                .createClaim(
                        request,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    @Test
    void createClaim_ShouldCompleteEmpty_WhenServiceReturnsEmpty() {

        CreateClaimRequest request =
                createClaimRequest();

        when(claimService.createClaim(
                request,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        claimController.createClaim(
                                request,
                                buyer
                        )
                )
                .verifyComplete();

        verify(claimService)
                .createClaim(
                        request,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    // =========================================================
    // GET CLAIM
    // =========================================================

    @Test
    void getClaim_ShouldReturnClaim_ForBuyer() {

        Claim claim =
                createClaim(
                        ClaimStatus.DOCUMENTS_SUBMITTED.name()
                );

        when(claimService.getClaim(
                CLAIM_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimController.getClaim(
                                CLAIM_ID,
                                buyer
                        )
                )
                .assertNext(response -> {

                    assertEquals(
                            CLAIM_ID,
                            response.getClaimId()
                    );

                    assertEquals(
                            POLICY_ID,
                            response.getPolicyId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            response.getApplicationId()
                    );

                    assertEquals(
                            ClaimStatus.DOCUMENTS_SUBMITTED.name(),
                            response.getStatus()
                    );
                })
                .verifyComplete();

        verify(claimService)
                .getClaim(
                        CLAIM_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    @Test
    void getClaim_ShouldReturnClaim_ForAdmin() {

        Claim claim =
                createClaim(
                        ClaimStatus.APPROVED.name()
                );

        when(claimService.getClaim(
                CLAIM_ID,
                1,
                "ADMIN"
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimController.getClaim(
                                CLAIM_ID,
                                admin
                        )
                )
                .expectNext(claim)
                .verifyComplete();

        verify(claimService)
                .getClaim(
                        CLAIM_ID,
                        1,
                        "ADMIN"
                );
    }

    @Test
    void getClaim_ShouldPropagateNotFoundError() {

        ClaimNotFoundException exception =
                new ClaimNotFoundException(
                        "Claim not found with ID: "
                                + CLAIM_ID
                );

        when(claimService.getClaim(
                CLAIM_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        claimController.getClaim(
                                CLAIM_ID,
                                buyer
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && (
                                "Claim not found with ID: "
                                        + CLAIM_ID
                        ).equals(error.getMessage())
                )
                .verify();

        verify(claimService)
                .getClaim(
                        CLAIM_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    @Test
    void getClaim_ShouldPropagateOwnershipError() {

        RuntimeException exception =
                new RuntimeException(
                        "You do not own this claim"
                );

        when(claimService.getClaim(
                CLAIM_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        claimController.getClaim(
                                CLAIM_ID,
                                buyer
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && "You do not own this claim"
                                .equals(error.getMessage())
                )
                .verify();

        verify(claimService)
                .getClaim(
                        CLAIM_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    // =========================================================
    // CALCULATE DELAY
    // =========================================================

    @Test
    void calculateDelay_ShouldReturnDelayVerifiedClaim() {

        Claim claim =
                createClaim(
                        ClaimStatus.DELAY_VERIFIED.name()
                );

        claim.setEligibleDelayMonths(
                9
        );

        claim.setRemarks(
                "Delay verified: 9 month(s)"
        );

        claim.setNew(false);

        when(claimService.calculateDelay(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimController.calculateDelay(
                                CLAIM_ID
                        )
                )
                .assertNext(response -> {

                    assertEquals(
                            9,
                            response.getEligibleDelayMonths()
                    );

                    assertEquals(
                            ClaimStatus.DELAY_VERIFIED.name(),
                            response.getStatus()
                    );

                    assertEquals(
                            "Delay verified: 9 month(s)",
                            response.getRemarks()
                    );
                })
                .verifyComplete();

        verify(claimService)
                .calculateDelay(CLAIM_ID);
    }

    @Test
    void calculateDelay_ShouldPropagateServiceError() {

        RuntimeException exception =
                new RuntimeException(
                        "Claim has no eligible delay"
                );

        when(claimService.calculateDelay(
                CLAIM_ID
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        claimController.calculateDelay(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && "Claim has no eligible delay"
                                .equals(error.getMessage())
                )
                .verify();

        verify(claimService)
                .calculateDelay(CLAIM_ID);
    }

    // =========================================================
    // CALCULATE COMPENSATION
    // =========================================================

    @Test
    void calculateCompensation_ShouldReturnCalculatedClaim() {

        Claim claim =
                createClaim(
                        ClaimStatus.DELAY_VERIFIED.name()
                );

        claim.setEligibleDelayMonths(
                9
        );

        claim.setApprovedAmount(
                450000.0
        );

        claim.setRemarks(
                "Compensation calculated for 9 eligible month(s)"
        );

        claim.setNew(false);

        when(claimService.calculateCompensation(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimController.calculateCompensation(
                                CLAIM_ID
                        )
                )
                .assertNext(response -> {

                    assertEquals(
                            450000.0,
                            response.getApprovedAmount()
                    );

                    assertEquals(
                            9,
                            response.getEligibleDelayMonths()
                    );

                    assertEquals(
                            ClaimStatus.DELAY_VERIFIED.name(),
                            response.getStatus()
                    );

                    assertEquals(
                            "Compensation calculated for "
                                    + "9 eligible month(s)",
                            response.getRemarks()
                    );
                })
                .verifyComplete();

        verify(claimService)
                .calculateCompensation(CLAIM_ID);
    }

    @Test
    void calculateCompensation_ShouldPropagateServiceError() {

        RuntimeException exception =
                new RuntimeException(
                        "Delay must be verified before "
                                + "compensation calculation"
                );

        when(claimService.calculateCompensation(
                CLAIM_ID
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        claimController.calculateCompensation(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && (
                                "Delay must be verified before "
                                        + "compensation calculation"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(claimService)
                .calculateCompensation(CLAIM_ID);
    }

    // =========================================================
    // VERIFY INSURANCE ELIGIBILITY
    // =========================================================

    @Test
    void verifyInsuranceEligibility_ShouldReturnVerifiedClaim() {

        Claim claim =
                createClaim(
                        ClaimStatus.ELIGIBILITY_VERIFIED.name()
                );

        claim.setEligibleDelayMonths(
                9
        );

        claim.setApprovedAmount(
                450000.0
        );

        claim.setRemarks(
                "Insurance eligibility verified"
        );

        claim.setNew(false);

        when(claimService.verifyInsuranceEligibility(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimController.verifyInsuranceEligibility(
                                CLAIM_ID
                        )
                )
                .assertNext(response -> {

                    assertEquals(
                            ClaimStatus.ELIGIBILITY_VERIFIED.name(),
                            response.getStatus()
                    );

                    assertEquals(
                            "Insurance eligibility verified",
                            response.getRemarks()
                    );
                })
                .verifyComplete();

        verify(claimService)
                .verifyInsuranceEligibility(CLAIM_ID);
    }

    @Test
    void verifyInsuranceEligibility_ShouldPropagateServiceError() {

        RuntimeException exception =
                new RuntimeException(
                        "Compensation must be calculated first"
                );

        when(claimService.verifyInsuranceEligibility(
                CLAIM_ID
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        claimController.verifyInsuranceEligibility(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && "Compensation must be calculated first"
                                .equals(error.getMessage())
                )
                .verify();

        verify(claimService)
                .verifyInsuranceEligibility(CLAIM_ID);
    }

    // =========================================================
    // APPROVE CLAIM
    // =========================================================

    @Test
    void approveClaim_ShouldReturnApprovedClaim() {

        Claim claim =
                createClaim(
                        ClaimStatus.APPROVED.name()
                );

        claim.setEligibleDelayMonths(
                9
        );

        claim.setApprovedAmount(
                450000.0
        );

        claim.setRemarks(
                "Claim approved"
        );

        claim.setNew(false);

        when(claimService.approveClaim(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimController.approveClaim(
                                CLAIM_ID
                        )
                )
                .assertNext(response -> {

                    assertEquals(
                            ClaimStatus.APPROVED.name(),
                            response.getStatus()
                    );

                    assertEquals(
                            450000.0,
                            response.getApprovedAmount()
                    );

                    assertFalse(
                            response.isNew()
                    );
                })
                .verifyComplete();

        verify(claimService)
                .approveClaim(CLAIM_ID);
    }

    @Test
    void approveClaim_ShouldPropagateServiceError() {

        RuntimeException exception =
                new RuntimeException(
                        "Claim must be ELIGIBILITY_VERIFIED "
                                + "before approval"
                );

        when(claimService.approveClaim(
                CLAIM_ID
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        claimController.approveClaim(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && (
                                "Claim must be ELIGIBILITY_VERIFIED "
                                        + "before approval"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(claimService)
                .approveClaim(CLAIM_ID);
    }

    // =========================================================
    // REJECT CLAIM
    // =========================================================

    @Test
    void rejectClaim_ShouldReturnRejectedClaim() {

        Claim claim =
                createClaim(
                        ClaimStatus.REJECTED.name()
                );

        claim.setRemarks(
                "Claim rejected"
        );

        claim.setNew(false);

        when(claimService.rejectClaim(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimController.rejectClaim(
                                CLAIM_ID
                        )
                )
                .assertNext(response -> {

                    assertEquals(
                            ClaimStatus.REJECTED.name(),
                            response.getStatus()
                    );

                    assertEquals(
                            "Claim rejected",
                            response.getRemarks()
                    );

                    assertFalse(
                            response.isNew()
                    );
                })
                .verifyComplete();

        verify(claimService)
                .rejectClaim(CLAIM_ID);
    }

    @Test
    void rejectClaim_ShouldPropagateServiceError() {

        RuntimeException exception =
                new RuntimeException(
                        "Claim is already finalized as APPROVED"
                );

        when(claimService.rejectClaim(
                CLAIM_ID
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        claimController.rejectClaim(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && "Claim is already finalized as APPROVED"
                                .equals(error.getMessage())
                )
                .verify();

        verify(claimService)
                .rejectClaim(CLAIM_ID);
    }

    // =========================================================
    // CLOSE CLAIM
    // =========================================================

    @Test
    void closeClaim_ShouldReturnClosedClaim() {

        Claim claim =
                createClaim(
                        ClaimStatus.CLOSED.name()
                );

        claim.setApprovedAmount(
                450000.0
        );

        claim.setRemarks(
                "Claim closed after compensation settlement"
        );

        claim.setNew(false);

        when(claimService.closeClaim(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimController.closeClaim(
                                CLAIM_ID
                        )
                )
                .assertNext(response -> {

                    assertEquals(
                            ClaimStatus.CLOSED.name(),
                            response.getStatus()
                    );

                    assertEquals(
                            "Claim closed after compensation settlement",
                            response.getRemarks()
                    );

                    assertFalse(
                            response.isNew()
                    );
                })
                .verifyComplete();

        verify(claimService)
                .closeClaim(CLAIM_ID);
    }

    @Test
    void closeClaim_ShouldPropagateServiceError() {

        RuntimeException exception =
                new RuntimeException(
                        "Claim is already finalized as CLOSED"
                );

        when(claimService.closeClaim(
                CLAIM_ID
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        claimController.closeClaim(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && "Claim is already finalized as CLOSED"
                                .equals(error.getMessage())
                )
                .verify();

        verify(claimService)
                .closeClaim(CLAIM_ID);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private CreateClaimRequest createClaimRequest() {

        CreateClaimRequest request =
                new CreateClaimRequest();

        request.setPolicyId(
                POLICY_ID
        );

        request.setReason(
                "Possession of the insured property was delayed"
        );

        request.setClaimedAmount(
                500000.0
        );

        request.setActualPossessionDate(
                LocalDate.of(
                        2026,
                        10,
                        4
                )
        );

        return request;
    }

    private Claim createClaim(
            String status
    ) {
        Claim claim =
                new Claim();

        claim.setClaimId(
                CLAIM_ID
        );

        claim.setPolicyId(
                POLICY_ID
        );

        claim.setApplicationId(
                APPLICATION_ID
        );

        claim.setReason(
                "Possession of the insured property was delayed"
        );

        claim.setClaimedAmount(
                500000.0
        );

        claim.setStatus(
                status
        );

        claim.setRemarks(
                "Claim submitted for review"
        );

        claim.setActualPossessionDate(
                LocalDate.of(
                        2026,
                        10,
                        4
                )
        );

        claim.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        10,
                        5,
                        1,
                        29
                )
        );

        claim.setNew(
                ClaimStatus.DOCUMENTS_SUBMITTED.name()
                        .equals(status)
        );

        return claim;
    }
}