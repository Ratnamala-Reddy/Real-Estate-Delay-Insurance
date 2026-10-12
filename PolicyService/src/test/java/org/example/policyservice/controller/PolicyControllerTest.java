package org.example.policyservice.controller;

import org.example.policyservice.dto.CreatePolicyRequest;
import org.example.policyservice.entity.Policy;
import org.example.policyservice.enums.PolicyStatus;
import org.example.policyservice.exception.PolicyNotFoundException;
import org.example.policyservice.security.AuthenticatedUser;
import org.example.policyservice.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyControllerTest {

    @Mock
    private PolicyService policyService;

    private PolicyController policyController;

    private AuthenticatedUser buyer;

    private AuthenticatedUser admin;

    private static final String POLICY_ID =
            "policy-101";

    private static final String QUOTE_ID =
            "quote-101";

    private static final String PAYMENT_ID =
            "payment-101";

    private static final String APPLICATION_ID =
            "application-101";

    @BeforeEach
    void setUp() {

        policyController =
                new PolicyController(
                        policyService
                );

        buyer =
                new AuthenticatedUser(
                        4,
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
    // CREATE POLICY
    // =========================================================

    @Test
    void createPolicy_ShouldReturnActivePolicy() {

        CreatePolicyRequest request =
                createPolicyRequest();

        Policy policy =
                createPolicy(
                        PolicyStatus.ACTIVE.name()
                );

        when(policyService.createPolicy(
                request
        )).thenReturn(
                Mono.just(policy)
        );

        StepVerifier.create(
                        policyController.createPolicy(
                                request
                        )
                )
                .assertNext(response -> {

                    assertEquals(
                            POLICY_ID,
                            response.getPolicyId()
                    );

                    assertEquals(
                            QUOTE_ID,
                            response.getQuoteId()
                    );

                    assertEquals(
                            PAYMENT_ID,
                            response.getPaymentId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            response.getApplicationId()
                    );

                    assertEquals(
                            PolicyStatus.ACTIVE.name(),
                            response.getPolicyStatus()
                    );
                })
                .verifyComplete();

        verify(policyService)
                .createPolicy(request);
    }

    @Test
    void createPolicy_ShouldPropagateServiceError() {

        CreatePolicyRequest request =
                createPolicyRequest();

        RuntimeException exception =
                new RuntimeException(
                        "Policy creation failed"
                );

        when(policyService.createPolicy(
                request
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        policyController.createPolicy(
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && "Policy creation failed"
                                .equals(error.getMessage())
                )
                .verify();

        verify(policyService)
                .createPolicy(request);
    }

    @Test
    void createPolicy_ShouldCompleteEmpty_WhenServiceReturnsEmpty() {

        CreatePolicyRequest request =
                createPolicyRequest();

        when(policyService.createPolicy(
                request
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        policyController.createPolicy(
                                request
                        )
                )
                .verifyComplete();

        verify(policyService)
                .createPolicy(request);
    }

    // =========================================================
    // GET ALL POLICIES
    // =========================================================

    @Test
    void getPolicies_ShouldReturnAllPolicies() {

        Policy firstPolicy =
                createPolicy(
                        PolicyStatus.ACTIVE.name()
                );

        Policy secondPolicy =
                createPolicy(
                        PolicyStatus.COMPLETED.name()
                );

        secondPolicy.setPolicyId(
                "policy-102"
        );

        secondPolicy.setQuoteId(
                "quote-102"
        );

        secondPolicy.setPaymentId(
                "payment-102"
        );

        when(policyService.getPolicies(
                1,
                "ADMIN"
        )).thenReturn(
                Flux.just(
                        firstPolicy,
                        secondPolicy
                )
        );

        StepVerifier.create(
                        policyController.getPolicies(admin)
                )
                .assertNext(policy ->
                        assertEquals(
                                POLICY_ID,
                                policy.getPolicyId()
                        )
                )
                .assertNext(policy ->
                        assertEquals(
                                "policy-102",
                                policy.getPolicyId()
                        )
                )
                .verifyComplete();

        verify(policyService)
                .getPolicies(1, "ADMIN");
    }

    @Test
    void getPolicies_ShouldCompleteEmpty_WhenNoPoliciesExist() {

        when(policyService.getPolicies(
                1,
                "ADMIN"
        )).thenReturn(
                Flux.empty()
        );

        StepVerifier.create(
                        policyController.getPolicies(admin)
                )
                .verifyComplete();

        verify(policyService)
                .getPolicies(1, "ADMIN");
    }

    // =========================================================
    // GET SINGLE POLICY
    // =========================================================

    @Test
    void getPolicy_ShouldReturnPolicy_ForBuyer() {

        Policy policy =
                createPolicy(
                        PolicyStatus.ACTIVE.name()
                );

        when(policyService.getPolicy(
                POLICY_ID,
                4,
                "BUYER"
        )).thenReturn(
                Mono.just(policy)
        );

        StepVerifier.create(
                        policyController.getPolicy(
                                POLICY_ID,
                                buyer
                        )
                )
                .assertNext(response -> {

                    assertEquals(
                            POLICY_ID,
                            response.getPolicyId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            response.getApplicationId()
                    );

                    assertEquals(
                            PolicyStatus.ACTIVE.name(),
                            response.getPolicyStatus()
                    );
                })
                .verifyComplete();

        verify(policyService)
                .getPolicy(
                        POLICY_ID,
                        4,
                        "BUYER"
                );
    }

    @Test
    void getPolicy_ShouldReturnPolicy_ForAdmin() {

        Policy policy =
                createPolicy(
                        PolicyStatus.ACTIVE.name()
                );

        when(policyService.getPolicy(
                POLICY_ID,
                1,
                "ADMIN"
        )).thenReturn(
                Mono.just(policy)
        );

        StepVerifier.create(
                        policyController.getPolicy(
                                POLICY_ID,
                                admin
                        )
                )
                .expectNext(policy)
                .verifyComplete();

        verify(policyService)
                .getPolicy(
                        POLICY_ID,
                        1,
                        "ADMIN"
                );
    }

    @Test
    void getPolicy_ShouldPropagateNotFoundError() {

        PolicyNotFoundException exception =
                new PolicyNotFoundException(
                        "Policy not found with ID: "
                                + POLICY_ID
                );

        when(policyService.getPolicy(
                POLICY_ID,
                4,
                "BUYER"
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        policyController.getPolicy(
                                POLICY_ID,
                                buyer
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && (
                                "Policy not found with ID: "
                                        + POLICY_ID
                        ).equals(error.getMessage())
                )
                .verify();

        verify(policyService)
                .getPolicy(
                        POLICY_ID,
                        4,
                        "BUYER"
                );
    }

    @Test
    void getPolicy_ShouldPropagateOwnershipError() {

        RuntimeException exception =
                new RuntimeException(
                        "You do not own this application"
                );

        when(policyService.getPolicy(
                POLICY_ID,
                4,
                "BUYER"
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        policyController.getPolicy(
                                POLICY_ID,
                                buyer
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && "You do not own this application"
                                .equals(error.getMessage())
                )
                .verify();

        verify(policyService)
                .getPolicy(
                        POLICY_ID,
                        4,
                        "BUYER"
                );
    }

    // =========================================================
    // COMPLETE POLICY
    // =========================================================

    @Test
    void completePolicy_ShouldReturnCompletedPolicy() {

        Policy policy =
                createPolicy(
                        PolicyStatus.COMPLETED.name()
                );

        policy.setNew(false);

        when(policyService.completePolicy(
                POLICY_ID
        )).thenReturn(
                Mono.just(policy)
        );

        StepVerifier.create(
                        policyController.completePolicy(
                                POLICY_ID
                        )
                )
                .assertNext(response -> {

                    assertEquals(
                            POLICY_ID,
                            response.getPolicyId()
                    );

                    assertEquals(
                            PolicyStatus.COMPLETED.name(),
                            response.getPolicyStatus()
                    );

                    assertFalse(
                            response.isNew()
                    );
                })
                .verifyComplete();

        verify(policyService)
                .completePolicy(POLICY_ID);
    }

    @Test
    void completePolicy_ShouldPropagateServiceError() {

        RuntimeException exception =
                new RuntimeException(
                        "Only ACTIVE policies can be completed"
                );

        when(policyService.completePolicy(
                POLICY_ID
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        policyController.completePolicy(
                                POLICY_ID
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && "Only ACTIVE policies can be completed"
                                .equals(error.getMessage())
                )
                .verify();

        verify(policyService)
                .completePolicy(POLICY_ID);
    }

    // =========================================================
    // CLOSE POLICY
    // =========================================================

    @Test
    void closePolicy_ShouldReturnClosedPolicy() {

        Policy policy =
                createPolicy(
                        PolicyStatus.CLOSED.name()
                );

        policy.setNew(false);

        when(policyService.closePolicy(
                POLICY_ID
        )).thenReturn(
                Mono.just(policy)
        );

        StepVerifier.create(
                        policyController.closePolicy(
                                POLICY_ID
                        )
                )
                .assertNext(response -> {

                    assertEquals(
                            POLICY_ID,
                            response.getPolicyId()
                    );

                    assertEquals(
                            PolicyStatus.CLOSED.name(),
                            response.getPolicyStatus()
                    );

                    assertFalse(
                            response.isNew()
                    );
                })
                .verifyComplete();

        verify(policyService)
                .closePolicy(POLICY_ID);
    }

    @Test
    void closePolicy_ShouldPropagateServiceError() {

        RuntimeException exception =
                new RuntimeException(
                        "Policy is already finalized as CLOSED"
                );

        when(policyService.closePolicy(
                POLICY_ID
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        policyController.closePolicy(
                                POLICY_ID
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && "Policy is already finalized as CLOSED"
                                .equals(error.getMessage())
                )
                .verify();

        verify(policyService)
                .closePolicy(POLICY_ID);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private CreatePolicyRequest createPolicyRequest() {

        CreatePolicyRequest request =
                new CreatePolicyRequest();

        request.setQuoteId(
                QUOTE_ID
        );

        request.setPaymentId(
                PAYMENT_ID
        );

        return request;
    }

    private Policy createPolicy(
            String status
    ) {
        Policy policy =
                new Policy();

        policy.setPolicyId(
                POLICY_ID
        );

        policy.setApplicationId(
                APPLICATION_ID
        );

        policy.setQuoteId(
                QUOTE_ID
        );

        policy.setPaymentId(
                PAYMENT_ID
        );

        policy.setPolicyNumber(
                "POL-ABC12345"
        );

        policy.setCoverageAmount(
                1200000.0
        );

        policy.setPremiumAmount(
                37500.0
        );

        policy.setPolicyStatus(
                status
        );

        policy.setStartDate(
                LocalDate.of(
                        2026,
                        10,
                        4
                )
        );

        policy.setEndDate(
                LocalDate.of(
                        2028,
                        10,
                        4
                )
        );

        policy.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        10,
                        4,
                        23,
                        59
                )
        );

        policy.setNew(
                PolicyStatus.ACTIVE.name()
                        .equals(status)
        );

        return policy;
    }
}