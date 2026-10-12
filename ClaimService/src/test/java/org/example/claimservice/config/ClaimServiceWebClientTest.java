package org.example.claimservice.config;

import org.example.claimservice.dto.CreateClaimRequest;
import org.example.claimservice.entity.Claim;
import org.example.claimservice.enums.ClaimStatus;
import org.example.claimservice.exceptions.ApplicationServiceException;
import org.example.claimservice.exceptions.ClaimNotEligibleException;
import org.example.claimservice.exceptions.PaymentServiceException;
import org.example.claimservice.exceptions.PolicyNotEligibleForClaimException;
import org.example.claimservice.exceptions.PolicyServiceException;
import org.example.claimservice.repository.ClaimRepository;
import org.example.claimservice.security.JwtService;
import org.example.claimservice.service.ClaimServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveInsertOperation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceWebClientTest {

    @Mock
    private ExchangeFunction exchangeFunction;

    @Mock
    private R2dbcEntityTemplate template;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private ReactiveInsertOperation.ReactiveInsert<Claim>
            claimInsert;

    private ClaimServiceImpl claimService;

    private static final String CLAIM_ID =
            "claim-101";

    private static final String POLICY_ID =
            "policy-101";

    private static final String APPLICATION_ID =
            "application-101";

    private static final String QUOTE_ID =
            "quote-101";

    private static final String SERVICE_TOKEN =
            "signed-claim-service-token";

    private static final Integer OWNER_USER_ID =
            4;

    @BeforeEach
    void setUp() {

        StepVerifier.setDefaultTimeout(
                Duration.ofSeconds(5)
        );

        WebClient webClient =
                WebClient.builder()
                        .exchangeFunction(
                                exchangeFunction
                        )
                        .build();

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
    // CREATE CLAIM SUCCESS
    // =========================================================

    @Test
    void createClaim_ShouldCreateClaim_WhenPolicyAndOwnerAreValid() {

        CreateClaimRequest request =
                createClaimRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(claimRepository.existsByPolicyId(
                POLICY_ID
        )).thenReturn(
                Mono.just(false)
        );

        List<ClientRequest> capturedRequests =
                new ArrayList<>();

        AtomicInteger callNumber =
                new AtomicInteger();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            ClientRequest clientRequest =
                    invocation.getArgument(0);

            capturedRequests.add(
                    clientRequest
            );

            if (callNumber.incrementAndGet() == 1) {
                return Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                activePolicyJson()
                        )
                );
            }

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            ownerApplicationJson()
                    )
            );
        });

        when(template.insert(
                Claim.class
        )).thenReturn(
                claimInsert
        );

        when(claimInsert.using(
                any(Claim.class)
        )).thenAnswer(invocation -> {

            Claim claim =
                    invocation.getArgument(0);

            claim.setClaimId(
                    CLAIM_ID
            );

            return Mono.just(claim);
        });

        StepVerifier.create(
                        claimService.createClaim(
                                request,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .assertNext(claim -> {

                    assertEquals(
                            CLAIM_ID,
                            claim.getClaimId()
                    );

                    assertEquals(
                            POLICY_ID,
                            claim.getPolicyId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            claim.getApplicationId()
                    );

                    assertEquals(
                            500000.0,
                            claim.getClaimedAmount()
                    );

                    assertEquals(
                            ClaimStatus.DOCUMENTS_SUBMITTED.name(),
                            claim.getStatus()
                    );

                    assertEquals(
                            "Claim submitted for review",
                            claim.getRemarks()
                    );

                    assertNotNull(
                            claim.getCreatedAt()
                    );
                })
                .verifyComplete();

        assertEquals(
                2,
                capturedRequests.size()
        );

        ClientRequest policyRequest =
                capturedRequests.get(0);

        ClientRequest applicationRequest =
                capturedRequests.get(1);

        assertEquals(
                HttpMethod.GET,
                policyRequest.method()
        );

        assertEquals(
                "http://localhost:8085/api/v1/policies/"
                        + POLICY_ID,
                policyRequest.url().toString()
        );

        assertEquals(
                "Bearer " + SERVICE_TOKEN,
                policyRequest
                        .headers()
                        .getFirst(
                                HttpHeaders.AUTHORIZATION
                        )
        );

        assertEquals(
                HttpMethod.GET,
                applicationRequest.method()
        );

        assertEquals(
                "http://localhost:8082/api/v1/applications/"
                        + APPLICATION_ID,
                applicationRequest.url().toString()
        );

        assertEquals(
                "Bearer " + SERVICE_TOKEN,
                applicationRequest
                        .headers()
                        .getFirst(
                                HttpHeaders.AUTHORIZATION
                        )
        );

        verify(claimInsert)
                .using(
                        any(Claim.class)
                );
    }

    // =========================================================
    // POLICY NOT ACTIVE
    // =========================================================

    @Test
    void createClaim_ShouldRejectClosedPolicy() {

        CreateClaimRequest request =
                createClaimRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(claimRepository.existsByPolicyId(
                POLICY_ID
        )).thenReturn(
                Mono.just(false)
        );

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                closedPolicyJson()
                        )
                )
        );

        StepVerifier.create(
                        claimService.createClaim(
                                request,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                PolicyNotEligibleForClaimException
                                && (
                                "Policy must be ACTIVE "
                                        + "before claim creation. "
                                        + "Current status: CLOSED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(Claim.class);
    }

    // =========================================================
    // POLICY NOT FOUND
    // =========================================================

    @Test
    void createClaim_ShouldMapPolicyNotFoundResponse() {

        CreateClaimRequest request =
                createClaimRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(claimRepository.existsByPolicyId(
                POLICY_ID
        )).thenReturn(
                Mono.just(false)
        );

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.NOT_FOUND,
                                """
                                {
                                  "code": "POLICY_NOT_FOUND",
                                  "message": "Policy not found"
                                }
                                """
                        )
                )
        );

        StepVerifier.create(
                        claimService.createClaim(
                                request,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PolicyServiceException
                                && (
                                "Policy not found: "
                                        + POLICY_ID
                        ).equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(Claim.class);
    }

    // =========================================================
    // POLICY ID MISMATCH
    // =========================================================

    @Test
    void createClaim_ShouldRejectPolicyIdMismatch() {

        CreateClaimRequest request =
                createClaimRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(claimRepository.existsByPolicyId(
                POLICY_ID
        )).thenReturn(
                Mono.just(false)
        );

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                """
                                {
                                  "policyId": "different-policy",
                                  "applicationId": "application-101",
                                  "quoteId": "quote-101",
                                  "policyStatus": "ACTIVE",
                                  "startDate": "2026-01-01",
                                  "endDate": "2028-01-01"
                                }
                                """
                        )
                )
        );

        StepVerifier.create(
                        claimService.createClaim(
                                request,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PolicyServiceException
                                && (
                                "Policy ID returned by Policy Service "
                                        + "does not match"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(Claim.class);
    }

    // =========================================================
    // OWNERSHIP FAILURE
    // =========================================================

    @Test
    void createClaim_ShouldThrowAccessDenied_WhenBuyerDoesNotOwnApplication() {

        CreateClaimRequest request =
                createClaimRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(claimRepository.existsByPolicyId(
                POLICY_ID
        )).thenReturn(
                Mono.just(false)
        );

        AtomicInteger callNumber =
                new AtomicInteger();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            if (callNumber.incrementAndGet() == 1) {
                return Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                activePolicyJson()
                        )
                );
            }

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            nonOwnerApplicationJson()
                    )
            );
        });

        StepVerifier.create(
                        claimService.createClaim(
                                request,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof AccessDeniedException
                                && (
                                "You do not own the application "
                                        + "associated with this policy"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(Claim.class);
    }

// =========================================================
// PROMISED DATE NOT PASSED
// =========================================================

    @Test
    void createClaim_ShouldRejectClaim_WhenPromisedDateHasNotPassed() {

        CreateClaimRequest request =
                createClaimRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(claimRepository.existsByPolicyId(
                POLICY_ID
        )).thenReturn(
                Mono.just(false)
        );

        AtomicInteger callNumber =
                new AtomicInteger();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            if (callNumber.incrementAndGet() == 1) {
                return Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                activePolicyJson()
                        )
                );
            }

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            futurePromisedDateApplicationJson()
                    )
            );
        });

        StepVerifier.create(
                        claimService.createClaim(
                                request,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ClaimNotEligibleException
                                && (
                                "A delay claim can be created only "
                                        + "after the promised "
                                        + "possession date"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(Claim.class);
    }

// =========================================================
// GET CLAIM OWNERSHIP SUCCESS
// =========================================================

    @Test
    void getClaim_ShouldReturnClaim_WhenBuyerOwnsApplication() {

        Claim claim =
                createClaim(
                        ClaimStatus.DOCUMENTS_SUBMITTED.name()
                );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                ownerApplicationJson()
                        )
                )
        );

        StepVerifier.create(
                        claimService.getClaim(
                                CLAIM_ID,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .expectNext(claim)
                .verifyComplete();

        verify(jwtService)
                .generateServiceToken();
    }

// =========================================================
// GET CLAIM OWNERSHIP FAILURE
// =========================================================

    @Test
    void getClaim_ShouldThrowAccessDenied_WhenBuyerDoesNotOwnClaim() {

        Claim claim =
                createClaim(
                        ClaimStatus.DOCUMENTS_SUBMITTED.name()
                );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                nonOwnerApplicationJson()
                        )
                )
        );

        StepVerifier.create(
                        claimService.getClaim(
                                CLAIM_ID,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof AccessDeniedException
                                && "You do not own this claim"
                                .equals(error.getMessage())
                )
                .verify();
    }

// =========================================================
// DELAY CALCULATION SUCCESS
// =========================================================

    @Test
    void calculateDelay_ShouldCalculateNineMonths() {

        Claim claim =
                createClaim(
                        ClaimStatus.DOCUMENTS_SUBMITTED.name()
                );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        AtomicInteger callNumber =
                new AtomicInteger();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            if (callNumber.incrementAndGet() == 1) {
                return Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                ownerApplicationJson()
                        )
                );
            }

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            activePolicyJson()
                    )
            );
        });

        when(template.update(
                claim
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.calculateDelay(
                                CLAIM_ID
                        )
                )
                .assertNext(result -> {

                    assertEquals(
                            9,
                            result.getEligibleDelayMonths()
                    );

                    assertEquals(
                            ClaimStatus.DELAY_VERIFIED.name(),
                            result.getStatus()
                    );

                    assertEquals(
                            "Delay verified: 9 month(s)",
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

// =========================================================
// COMPENSATION CALCULATION SUCCESS
// =========================================================

    @Test
    void calculateCompensation_ShouldUseQuoteTermsAndCaps() {

        Claim claim =
                createClaim(
                        ClaimStatus.DELAY_VERIFIED.name()
                );

        claim.setEligibleDelayMonths(
                9
        );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        AtomicInteger callNumber =
                new AtomicInteger();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            if (callNumber.incrementAndGet() == 1) {
                return Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                activePolicyJson()
                        )
                );
            }

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            acceptedQuoteJson()
                    )
            );
        });

        when(template.update(
                claim
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.calculateCompensation(
                                CLAIM_ID
                        )
                )
                .assertNext(result -> {

                    assertEquals(
                            450000.0,
                            result.getApprovedAmount()
                    );

                    assertEquals(
                            ClaimStatus.DELAY_VERIFIED.name(),
                            result.getStatus()
                    );

                    assertEquals(
                            "Compensation calculated for "
                                    + "9 eligible month(s)",
                            result.getRemarks()
                    );
                })
                .verifyComplete();

        verify(template)
                .update(claim);
    }

// =========================================================
// COMPENSATION CAPPED BY CLAIMED AMOUNT
// =========================================================

    @Test
    void calculateCompensation_ShouldCapAmountByClaimedAmount() {

        Claim claim =
                createClaim(
                        ClaimStatus.DELAY_VERIFIED.name()
                );

        claim.setEligibleDelayMonths(
                9
        );

        claim.setClaimedAmount(
                300000.0
        );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        AtomicInteger callNumber =
                new AtomicInteger();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            if (callNumber.incrementAndGet() == 1) {
                return Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                activePolicyJson()
                        )
                );
            }

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            acceptedQuoteJson()
                    )
            );
        });

        when(template.update(
                claim
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.calculateCompensation(
                                CLAIM_ID
                        )
                )
                .assertNext(result ->
                        assertEquals(
                                300000.0,
                                result.getApprovedAmount()
                        )
                )
                .verifyComplete();

        verify(template)
                .update(claim);
    }

// =========================================================
// ELIGIBILITY VERIFICATION SUCCESS
// =========================================================

    @Test
    void verifyInsuranceEligibility_ShouldUpdateClaim() {

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

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                activePolicyJson()
                        )
                )
        );

        when(template.update(
                claim
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.verifyInsuranceEligibility(
                                CLAIM_ID
                        )
                )
                .assertNext(result -> {

                    assertEquals(
                            ClaimStatus.ELIGIBILITY_VERIFIED.name(),
                            result.getStatus()
                    );

                    assertEquals(
                            "Insurance eligibility verified",
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

// =========================================================
// APPROVAL AND PAYMENT SERVICE SUCCESS
// =========================================================

    @Test
    void approveClaim_ShouldCreateCompensationPayment() {

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

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        AtomicReference<ClientRequest> capturedRequest =
                new AtomicReference<>();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            ClientRequest clientRequest =
                    invocation.getArgument(0);

            capturedRequest.set(
                    clientRequest
            );

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            """
                            {
                              "paymentId": "compensation-payment-101",
                              "claimId": "claim-101",
                              "policyId": "policy-101",
                              "payoutAmount": 450000.0,
                              "status": "INITIATED"
                            }
                            """
                    )
            );
        });

        when(template.update(
                claim
        )).thenReturn(
                Mono.just(claim)
        );

        StepVerifier.create(
                        claimService.approveClaim(
                                CLAIM_ID
                        )
                )
                .assertNext(result -> {

                    assertEquals(
                            ClaimStatus.APPROVED.name(),
                            result.getStatus()
                    );

                    assertEquals(
                            "Claim approved",
                            result.getRemarks()
                    );

                    assertEquals(
                            450000.0,
                            result.getApprovedAmount()
                    );

                    assertFalse(
                            result.isNew()
                    );
                })
                .verifyComplete();

        ClientRequest paymentRequest =
                capturedRequest.get();

        assertNotNull(paymentRequest);

        assertEquals(
                HttpMethod.POST,
                paymentRequest.method()
        );

        assertEquals(
                "http://localhost:8086/api/v1/compensation-payments",
                paymentRequest.url().toString()
        );

        assertEquals(
                "Bearer " + SERVICE_TOKEN,
                paymentRequest
                        .headers()
                        .getFirst(
                                HttpHeaders.AUTHORIZATION
                        )
        );

        verify(exchangeFunction)
                .exchange(
                        any(ClientRequest.class)
                );

        verify(template)
                .update(claim);
    }
// =========================================================
// APPROVAL AND PAYMENT SERVICE FAILURE
// =========================================================

    @Test
    void approveClaim_ShouldMapPaymentServiceFailure() {

        Claim claim =
                createClaim(
                        ClaimStatus.ELIGIBILITY_VERIFIED.name()
                );

        claim.setApprovedAmount(
                450000.0
        );

        when(claimRepository.findById(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                """
                                {
                                  "code": "INTERNAL_SERVER_ERROR",
                                  "message": "Payment creation failed"
                                }
                                """
                        )
                )
        );

        StepVerifier.create(
                        claimService.approveClaim(
                                CLAIM_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PaymentServiceException
                                && error.getMessage() != null
                                && error.getMessage().contains(
                                "Payment Service returned"
                        )
                                && error.getMessage().contains(
                                "500"
                        )
                                && error.getMessage().contains(
                                "Payment creation failed"
                        )
                )
                .verify();

        assertEquals(
                ClaimStatus.ELIGIBILITY_VERIFIED.name(),
                claim.getStatus()
        );

        verify(template, never())
                .update(any(Claim.class));
    }

// =========================================================
// HELPERS
// =========================================================

    private ClientResponse jsonResponse(
            HttpStatus status,
            String body
    ) {
        return ClientResponse
                .create(status)
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .body(body)
                .build();
    }

    private String activePolicyJson() {
        return """
            {
              "policyId": "policy-101",
              "applicationId": "application-101",
              "quoteId": "quote-101",
              "paymentId": "premium-payment-101",
              "policyNumber": "POL-ABC12345",
              "coverageAmount": 1200000.0,
              "premiumAmount": 37500.0,
              "policyStatus": "ACTIVE",
              "startDate": "2026-01-01",
              "endDate": "2028-01-01"
            }
            """;
    }

    private String closedPolicyJson() {
        return """
            {
              "policyId": "policy-101",
              "applicationId": "application-101",
              "quoteId": "quote-101",
              "paymentId": "premium-payment-101",
              "policyStatus": "CLOSED",
              "startDate": "2026-01-01",
              "endDate": "2028-01-01"
            }
            """;
    }

    private String ownerApplicationJson() {
        return """
            {
              "applicationId": "application-101",
              "userId": 4,
              "promisedPossessionDate": "2026-01-01",
              "status": "VERIFIED"
            }
            """;
    }

    private String nonOwnerApplicationJson() {
        return """
            {
              "applicationId": "application-101",
              "userId": 99,
              "promisedPossessionDate": "2026-01-01",
              "status": "VERIFIED"
            }
            """;
    }

    private String futurePromisedDateApplicationJson() {
        return """
            {
              "applicationId": "application-101",
              "userId": 4,
              "promisedPossessionDate": "2027-01-01",
              "status": "VERIFIED"
            }
            """;
    }

    private String acceptedQuoteJson() {
        return """
            {
              "quoteId": "quote-101",
              "applicationId": "application-101",
              "planName": "STANDARD",
              "premiumAmount": 37500.0,
              "monthlyCompensation": 50000.0,
              "maximumDelayMonths": 24,
              "maximumPayout": 1200000.0,
              "gracePeriodDays": 90,
              "status": "ACCEPTED"
            }
            """;
    }

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