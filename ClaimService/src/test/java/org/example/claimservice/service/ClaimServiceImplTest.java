package org.example.claimservice.service;

import org.example.claimservice.dto.CreateClaimRequest;
import org.example.claimservice.entity.Claim;
import org.example.claimservice.enums.ClaimStatus;
import org.example.claimservice.exceptions.ClaimAlreadyExistsException;
import org.example.claimservice.exceptions.ClaimAlreadyFinalizedException;
import org.example.claimservice.exceptions.ClaimNotFoundException;
import org.example.claimservice.exceptions.InvalidClaimRequestException;
import org.example.claimservice.exceptions.InvalidClaimStateException;
import org.example.claimservice.repository.ClaimRepository;
import org.example.claimservice.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceImplTest {

    @Mock
    private R2dbcEntityTemplate template;

    @Mock
    private WebClient webClient;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private JwtService jwtService;

    private ClaimServiceImpl claimService;

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

        StepVerifier.setDefaultTimeout(
                Duration.ofSeconds(5)
        );

        claimService =
                new ClaimServiceImpl(
                        template,
                        webClient,
                        claimRepository,
                        jwtService
                );

        ReflectionTestUtils.setField(
                claimService,
                "policyServiceUrl",
                "http://localhost:8085"
        );

        ReflectionTestUtils.setField(
                claimService,
                "applicationServiceUrl",
                "http://localhost:8082"
        );

        ReflectionTestUtils.setField(
                claimService,
                "paymentServiceUrl",
                "http://localhost:8086"
        );
    }

    @AfterEach
    void tearDown() {
        StepVerifier.resetDefaultTimeout();
    }

    // =========================================================
    // CREATE CLAIM REQUEST VALIDATION
    // =========================================================

    @Test
    void createClaim_ShouldThrowException_WhenRequestIsNull() {

        StepVerifier.create(
                        Mono.defer(() ->
                                claimService.createClaim(
                                        null,
                                        BUYER_USER_ID,
                                        "BUYER"
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidClaimRequestException
                                && "Create claim request is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void createClaim_ShouldThrowException_WhenPolicyIdIsNull() {

        CreateClaimRequest request =
                createClaimRequest();

        request.setPolicyId(null);

        StepVerifier.create(
                        Mono.defer(() ->
                                claimService.createClaim(
                                        request,
                                        BUYER_USER_ID,
                                        "BUYER"
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidClaimRequestException
                                && "Policy ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void createClaim_ShouldThrowException_WhenPolicyIdIsBlank() {

        CreateClaimRequest request =
                createClaimRequest();

        request.setPolicyId("   ");

        StepVerifier.create(
                        Mono.defer(() ->
                                claimService.createClaim(
                                        request,
                                        BUYER_USER_ID,
                                        "BUYER"
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidClaimRequestException
                                && "Policy ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void createClaim_ShouldThrowException_WhenReasonIsBlank() {

        CreateClaimRequest request =
                createClaimRequest();

        request.setReason("");

        StepVerifier.create(
                        Mono.defer(() ->
                                claimService.createClaim(
                                        request,
                                        BUYER_USER_ID,
                                        "BUYER"
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidClaimRequestException
                                && "Claim reason is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void createClaim_ShouldThrowException_WhenClaimedAmountIsNull() {

        CreateClaimRequest request =
                createClaimRequest();

        request.setClaimedAmount(null);

        StepVerifier.create(
                        Mono.defer(() ->
                                claimService.createClaim(
                                        request,
                                        BUYER_USER_ID,
                                        "BUYER"
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidClaimRequestException
                                && "Claimed amount must be greater than zero"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void createClaim_ShouldThrowException_WhenClaimedAmountIsZero() {

        CreateClaimRequest request =
                createClaimRequest();

        request.setClaimedAmount(0.0);

        StepVerifier.create(
                        Mono.defer(() ->
                                claimService.createClaim(
                                        request,
                                        BUYER_USER_ID,
                                        "BUYER"
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidClaimRequestException
                                && "Claimed amount must be greater than zero"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void createClaim_ShouldThrowException_WhenClaimedAmountIsNegative() {

        CreateClaimRequest request =
                createClaimRequest();

        request.setClaimedAmount(-100.0);

        StepVerifier.create(
                        Mono.defer(() ->
                                claimService.createClaim(
                                        request,
                                        BUYER_USER_ID,
                                        "BUYER"
                                )
                        )
                )
                .expectError(
                        InvalidClaimRequestException.class
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void createClaim_ShouldThrowException_WhenActualDateIsInFuture() {

        CreateClaimRequest request =
                createClaimRequest();

        request.setActualPossessionDate(
                LocalDate.now()
                        .plusDays(1)
        );

        StepVerifier.create(
                        Mono.defer(() ->
                                claimService.createClaim(
                                        request,
                                        BUYER_USER_ID,
                                        "BUYER"
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidClaimRequestException
                                && "Actual possession date cannot be in the future"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    // =========================================================
    // CREATE CLAIM AUTHORIZATION
    // =========================================================

    @Test
    void createClaim_ShouldThrowAccessDenied_WhenRoleIsAdmin() {

        CreateClaimRequest request =
                createClaimRequest();

        StepVerifier.create(
                        claimService.createClaim(
                                request,
                                1,
                                "ADMIN"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof AccessDeniedException
                                && "Only BUYER can create a claim"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void createClaim_ShouldThrowAccessDenied_WhenRoleIsUnsupported() {

        CreateClaimRequest request =
                createClaimRequest();

        StepVerifier.create(
                        claimService.createClaim(
                                request,
                                10,
                                "CLAIMS_OFFICER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof AccessDeniedException
                                && "Only BUYER can create a claim"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void createClaim_ShouldThrowAccessDenied_WhenBuyerUserIdIsNull() {

        CreateClaimRequest request =
                createClaimRequest();

        StepVerifier.create(
                        claimService.createClaim(
                                request,
                                null,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof AccessDeniedException
                                && "Caller user ID is missing"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    // =========================================================
    // DUPLICATE CLAIM
    // =========================================================

    @Test
    void createClaim_ShouldThrowException_WhenClaimAlreadyExists() {

        CreateClaimRequest request =
                createClaimRequest();

        when(jwtService.generateServiceToken())
                .thenReturn("service-token");

        when(claimRepository.existsByPolicyId(
                POLICY_ID
        )).thenReturn(
                Mono.just(true)
        );

        StepVerifier.create(
                        claimService.createClaim(
                                request,
                                BUYER_USER_ID,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ClaimAlreadyExistsException
                                && (
                                "Claim already exists for policy: "
                                        + POLICY_ID
                        ).equals(error.getMessage())
                )
                .verify();

        verify(claimRepository)
                .existsByPolicyId(POLICY_ID);

        verify(template, never())
                .insert(Claim.class);
    }

    // =========================================================
    // GET CLAIM AS ADMIN
    // =========================================================

    @Test
    void getClaim_ShouldReturnClaim_WhenCallerIsAdmin() {

        Claim claim =
                createClaim(
                        ClaimStatus.DOCUMENTS_SUBMITTED.name()
                );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.getClaim(
                                CLAIM_ID,
                                1,
                                "ADMIN"
                        )
                )
                .assertNext(result -> {

                    assertEquals(
                            CLAIM_ID,
                            result.getClaimId()
                    );

                    assertEquals(
                            POLICY_ID,
                            result.getPolicyId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            result.getApplicationId()
                    );

                    assertEquals(
                            ClaimStatus.DOCUMENTS_SUBMITTED.name(),
                            result.getStatus()
                    );
                })
                .verifyComplete();

        verify(claimRepository)
                .findById(CLAIM_ID);

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void getClaimWithoutIdentity_ShouldUseInternalAdminAccess() {

        Claim claim =
                createClaim(
                        ClaimStatus.APPROVED.name()
                );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.getClaim(
                                CLAIM_ID
                        )
                )
                .expectNext(claim)
                .verifyComplete();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void getClaim_ShouldThrowNotFound_WhenClaimDoesNotExist() {

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        claimService.getClaim(
                                CLAIM_ID,
                                1,
                                "ADMIN"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ClaimNotFoundException
                                && (
                                "Claim not found with ID: "
                                        + CLAIM_ID
                        ).equals(error.getMessage())
                )
                .verify();
    }

    @Test
    void getClaim_ShouldThrowException_WhenClaimIdIsBlank() {

        StepVerifier.create(
                        Mono.defer(() ->
                                claimService.getClaim(
                                        " ",
                                        1,
                                        "ADMIN"
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidClaimRequestException
                                && "Claim ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(claimRepository, never())
                .findById(any(String.class));
    }

    @Test
    void getClaim_ShouldThrowAccessDenied_WhenRoleIsUnsupported() {

        Claim claim =
                createClaim(
                        ClaimStatus.DOCUMENTS_SUBMITTED.name()
                );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.getClaim(
                                CLAIM_ID,
                                10,
                                "CLAIMS_OFFICER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof AccessDeniedException
                                && "Unsupported caller role"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void getClaim_ShouldThrowAccessDenied_WhenBuyerUserIdIsNull() {

        Claim claim =
                createClaim(
                        ClaimStatus.DOCUMENTS_SUBMITTED.name()
                );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.getClaim(
                                CLAIM_ID,
                                null,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof AccessDeniedException
                                && "Caller user ID is missing"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    // =========================================================
    // CALCULATE DELAY VALIDATION
    // =========================================================

    @Test
    void calculateDelay_ShouldThrowException_WhenClaimIdIsBlank() {

        StepVerifier.create(
                        Mono.defer(() ->
                                claimService.calculateDelay(" ")
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidClaimRequestException
                                && "Claim ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(claimRepository, never())
                .findById(any(String.class));
    }

    @Test
    void calculateDelay_ShouldThrowNotFound_WhenClaimDoesNotExist() {

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        claimService.calculateDelay(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ClaimNotFoundException
                                && (
                                "Claim not found with ID: "
                                        + CLAIM_ID
                        ).equals(error.getMessage())
                )
                .verify();
    }

    @Test
    void calculateDelay_ShouldRejectWrongClaimState() {

        Claim claim =
                createClaim(
                        ClaimStatus.ELIGIBILITY_VERIFIED.name()
                );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.calculateDelay(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidClaimStateException
                                && (
                                "Delay can be calculated only for "
                                        + "DRAFT or DOCUMENTS_SUBMITTED claims. "
                                        + "Current status: "
                                        + ClaimStatus.ELIGIBILITY_VERIFIED.name()
                        ).equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    // =========================================================
    // COMPENSATION VALIDATION
    // =========================================================

    @Test
    void calculateCompensation_ShouldRejectDocumentsSubmittedClaim() {

        Claim claim =
                createClaim(
                        ClaimStatus.DOCUMENTS_SUBMITTED.name()
                );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.calculateCompensation(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidClaimStateException
                                && (
                                "Delay must be verified before "
                                        + "compensation calculation. "
                                        + "Current status: DOCUMENTS_SUBMITTED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void calculateCompensation_ShouldRejectMissingEligibleDelayMonths() {

        Claim claim =
                createClaim(
                        ClaimStatus.DELAY_VERIFIED.name()
                );

        claim.setEligibleDelayMonths(null);

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.calculateCompensation(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidClaimStateException
                                && "Eligible delay months are missing"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    // =========================================================
    // INSURANCE VERIFICATION VALIDATION
    // =========================================================

    @Test
    void verifyInsuranceEligibility_ShouldRejectWrongState() {

        Claim claim =
                createClaim(
                        ClaimStatus.DOCUMENTS_SUBMITTED.name()
                );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.verifyInsuranceEligibility(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidClaimStateException
                                && (
                                "Delay must be verified before "
                                        + "insurance eligibility verification. "
                                        + "Current status: DOCUMENTS_SUBMITTED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void verifyInsuranceEligibility_ShouldRejectMissingApprovedAmount() {

        Claim claim =
                createClaim(
                        ClaimStatus.DELAY_VERIFIED.name()
                );

        claim.setEligibleDelayMonths(9);
        claim.setApprovedAmount(null);

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.verifyInsuranceEligibility(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidClaimStateException
                                && (
                                "Compensation must be calculated before "
                                        + "insurance eligibility verification"
                        ).equals(error.getMessage())
                )
                .verify();
    }

    // =========================================================
    // APPROVE CLAIM VALIDATION
    // =========================================================

    @Test
    void approveClaim_ShouldRejectWrongStatus() {

        Claim claim =
                createClaim(
                        ClaimStatus.DELAY_VERIFIED.name()
                );

        claim.setApprovedAmount(450000.0);

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.approveClaim(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidClaimStateException
                                && (
                                "Claim must be ELIGIBILITY_VERIFIED "
                                        + "before approval. "
                                        + "Current status: DELAY_VERIFIED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .update(any(Claim.class));
    }

    @Test
    void approveClaim_ShouldRejectMissingApprovedAmount() {

        Claim claim =
                createClaim(
                        ClaimStatus.ELIGIBILITY_VERIFIED.name()
                );

        claim.setApprovedAmount(null);

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.approveClaim(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidClaimStateException
                                && (
                                "Approved amount must be calculated "
                                        + "before claim approval"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .update(any(Claim.class));
    }

    // =========================================================
    // REJECT CLAIM
    // =========================================================

    @Test
    void rejectClaim_ShouldRejectClaimSuccessfully() {

        Claim claim =
                createClaim(
                        ClaimStatus.DOCUMENTS_SUBMITTED.name()
                );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        when(template.update(claim))
                .thenReturn(
                        Mono.just(claim)
                );

        StepVerifier.create(
                        claimService.rejectClaim(
                                CLAIM_ID
                        )
                )
                .assertNext(result -> {

                    assertEquals(
                            ClaimStatus.REJECTED.name(),
                            result.getStatus()
                    );

                    assertEquals(
                            "Claim rejected",
                            result.getRemarks()
                    );

                    assertFalse(
                            result.isNew()
                    );
                })
                .verifyComplete();

        verify(template)
                .update(claim);
    }

    @Test
    void rejectClaim_ShouldRejectAlreadyApprovedClaim() {

        Claim claim =
                createClaim(
                        ClaimStatus.APPROVED.name()
                );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.rejectClaim(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ClaimAlreadyFinalizedException
                                && "Claim is already finalized as APPROVED"
                                .equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .update(any(Claim.class));
    }

    @Test
    void rejectClaim_ShouldRejectAlreadyRejectedClaim() {

        Claim claim =
                createClaim(
                        ClaimStatus.REJECTED.name()
                );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.rejectClaim(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ClaimAlreadyFinalizedException
                                && "Claim is already finalized as REJECTED"
                                .equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .update(any(Claim.class));
    }

    // =========================================================
    // CLOSE CLAIM
    // =========================================================

    @Test
    void closeClaim_ShouldCloseApprovedClaim() {

        Claim claim =
                createClaim(
                        ClaimStatus.APPROVED.name()
                );

        claim.setApprovedAmount(450000.0);

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        when(template.update(claim))
                .thenReturn(
                        Mono.just(claim)
                );

        StepVerifier.create(
                        claimService.closeClaim(
                                CLAIM_ID
                        )
                )
                .assertNext(result -> {

                    assertEquals(
                            ClaimStatus.CLOSED.name(),
                            result.getStatus()
                    );

                    assertEquals(
                            "Claim closed after compensation settlement",
                            result.getRemarks()
                    );

                    assertFalse(
                            result.isNew()
                    );
                })
                .verifyComplete();

        verify(template)
                .update(claim);
    }

    @Test
    void closeClaim_ShouldCloseSettledClaim() {

        Claim claim =
                createClaim(
                        ClaimStatus.SETTLED.name()
                );

        claim.setApprovedAmount(450000.0);

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        when(template.update(claim))
                .thenReturn(
                        Mono.just(claim)
                );

        StepVerifier.create(
                        claimService.closeClaim(
                                CLAIM_ID
                        )
                )
                .assertNext(result ->
                        assertEquals(
                                ClaimStatus.CLOSED.name(),
                                result.getStatus()
                        )
                )
                .verifyComplete();

        verify(template)
                .update(claim);
    }

    @Test
    void closeClaim_ShouldRejectAlreadyClosedClaim() {

        Claim claim =
                createClaim(
                        ClaimStatus.CLOSED.name()
                );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.closeClaim(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ClaimAlreadyFinalizedException
                                && "Claim is already finalized as CLOSED"
                                .equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .update(any(Claim.class));
    }

    @Test
    void closeClaim_ShouldRejectDocumentsSubmittedClaim() {

        Claim claim =
                createClaim(
                        ClaimStatus.DOCUMENTS_SUBMITTED.name()
                );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.closeClaim(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidClaimStateException
                                && (
                                "Only APPROVED or SETTLED claims "
                                        + "can be closed. "
                                        + "Current status: DOCUMENTS_SUBMITTED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .update(any(Claim.class));
    }

    @Test
    void closeClaim_ShouldThrowNotFound_WhenClaimDoesNotExist() {

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        claimService.closeClaim(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ClaimNotFoundException
                                && (
                                "Claim not found with ID: "
                                        + CLAIM_ID
                        ).equals(error.getMessage())
                )
                .verify();
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

        claim.setActualPossessionDate(
                LocalDate.of(
                        2026,
                        10,
                        4
                )
        );

        claim.setStatus(
                status
        );

        claim.setRemarks(
                "Claim submitted for review"
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