package org.example.riskservice.controller;

import org.example.riskservice.dto.ApproveRiskRequest;
import org.example.riskservice.dto.DeclineRiskRequest;
import org.example.riskservice.dto.RiskAssessmentRequest;
import org.example.riskservice.entity.RiskAssessment;
import org.example.riskservice.enums.RiskAssessmentStatus;
import org.example.riskservice.enums.RiskCategory;
import org.example.riskservice.exceptions.RiskAssessmentNotFoundException;
import org.example.riskservice.service.RiskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskControllerTest {

    @Mock
    private RiskService riskService;

    private RiskController riskController;

    private static final String APPLICATION_ID =
            "application-101";

    private static final String ASSESSMENT_ID =
            "assessment-101";

    @BeforeEach
    void setUp() {
        riskController =
                new RiskController(
                        riskService
                );
    }

    // =========================================================
    // CALCULATE RISK
    // =========================================================

    @Test
    void calculateRisk_ShouldReturnCreatedAssessment() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        RiskAssessment assessment =
                createRiskAssessment(
                        RiskAssessmentStatus.PENDING.name()
                );

        when(riskService.calculateRisk(request))
                .thenReturn(
                        Mono.just(assessment)
                );

        StepVerifier.create(
                        riskController.calculateRisk(request)
                )
                .assertNext(response -> {
                    assertEquals(
                            ASSESSMENT_ID,
                            response.getAssessmentId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            response.getApplicationId()
                    );

                    assertEquals(
                            80,
                            response.getRiskScore()
                    );

                    assertEquals(
                            RiskCategory.LOW.name(),
                            response.getRiskCategory()
                    );

                    assertEquals(
                            1.0,
                            response.getPremiumMultiplier()
                    );

                    assertEquals(
                            RiskAssessmentStatus.PENDING.name(),
                            response.getStatus()
                    );

                    assertEquals(
                            "ADMIN",
                            response.getAssessedBy()
                    );
                })
                .verifyComplete();

        verify(riskService)
                .calculateRisk(request);
    }

    @Test
    void calculateRisk_ShouldPropagateServiceError() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        RuntimeException exception =
                new RuntimeException(
                        "Risk calculation failed"
                );

        when(riskService.calculateRisk(request))
                .thenReturn(
                        Mono.error(exception)
                );

        StepVerifier.create(
                        riskController.calculateRisk(request)
                )
                .expectErrorMatches(error ->
                        error == exception
                                && error.getMessage().equals(
                                "Risk calculation failed"
                        )
                )
                .verify();

        verify(riskService)
                .calculateRisk(request);
    }

    // =========================================================
    // GET RISK ASSESSMENT
    // =========================================================

    @Test
    void getRiskAssessment_ShouldReturnAssessment() {

        RiskAssessment assessment =
                createRiskAssessment(
                        RiskAssessmentStatus.PENDING.name()
                );

        when(riskService.getRiskAssessment(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(assessment)
        );

        StepVerifier.create(
                        riskController.getRiskAssessment(
                                APPLICATION_ID
                        )
                )
                .assertNext(response -> {
                    assertEquals(
                            ASSESSMENT_ID,
                            response.getAssessmentId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            response.getApplicationId()
                    );

                    assertEquals(
                            RiskAssessmentStatus.PENDING.name(),
                            response.getStatus()
                    );
                })
                .verifyComplete();

        verify(riskService)
                .getRiskAssessment(
                        APPLICATION_ID
                );
    }

    @Test
    void getRiskAssessment_ShouldPropagateNotFoundError() {

        RiskAssessmentNotFoundException exception =
                new RiskAssessmentNotFoundException(
                        "No risk assessment found for application: "
                                + APPLICATION_ID
                );

        when(riskService.getRiskAssessment(
                APPLICATION_ID
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        riskController.getRiskAssessment(
                                APPLICATION_ID
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && error.getMessage().equals(
                                "No risk assessment found for application: "
                                        + APPLICATION_ID
                        )
                )
                .verify();

        verify(riskService)
                .getRiskAssessment(
                        APPLICATION_ID
                );
    }

    // =========================================================
    // APPROVE RISK ASSESSMENT
    // =========================================================

    @Test
    void approveRiskAssessment_ShouldReturnApprovedAssessment() {

        ApproveRiskRequest request =
                createApproveRiskRequest();

        RiskAssessment assessment =
                createRiskAssessment(
                        RiskAssessmentStatus.APPROVED.name()
                );

        assessment.setRemarks(
                request.getRemarks()
        );

        assessment.setAssessedBy(
                request.getApprovedBy()
        );

        when(riskService.approveRiskAssessment(
                ASSESSMENT_ID,
                request
        )).thenReturn(
                Mono.just(assessment)
        );

        StepVerifier.create(
                        riskController.approveRiskAssessment(
                                ASSESSMENT_ID,
                                request
                        )
                )
                .assertNext(response -> {
                    assertEquals(
                            ASSESSMENT_ID,
                            response.getAssessmentId()
                    );

                    assertEquals(
                            RiskAssessmentStatus.APPROVED.name(),
                            response.getStatus()
                    );

                    assertEquals(
                            "Risk assessment reviewed and approved",
                            response.getRemarks()
                    );

                    assertEquals(
                            "ADMIN",
                            response.getAssessedBy()
                    );

                    assertFalse(
                            response.isNew()
                    );
                })
                .verifyComplete();

        verify(riskService)
                .approveRiskAssessment(
                        ASSESSMENT_ID,
                        request
                );
    }

    @Test
    void approveRiskAssessment_ShouldPropagateServiceError() {

        ApproveRiskRequest request =
                createApproveRiskRequest();

        RuntimeException exception =
                new RuntimeException(
                        "Unable to generate quote"
                );

        when(riskService.approveRiskAssessment(
                ASSESSMENT_ID,
                request
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        riskController.approveRiskAssessment(
                                ASSESSMENT_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && error.getMessage().equals(
                                "Unable to generate quote"
                        )
                )
                .verify();

        verify(riskService)
                .approveRiskAssessment(
                        ASSESSMENT_ID,
                        request
                );
    }

    // =========================================================
    // DECLINE RISK ASSESSMENT
    // =========================================================

    @Test
    void declineRiskAssessment_ShouldReturnDeclinedAssessment() {

        DeclineRiskRequest request =
                createDeclineRiskRequest();

        RiskAssessment assessment =
                createRiskAssessment(
                        RiskAssessmentStatus.DECLINED.name()
                );

        assessment.setRemarks(
                request.getRemarks()
        );

        assessment.setAssessedBy(
                request.getDeclinedBy()
        );

        when(riskService.declineRiskAssessment(
                ASSESSMENT_ID,
                request
        )).thenReturn(
                Mono.just(assessment)
        );

        StepVerifier.create(
                        riskController.declineRiskAssessment(
                                ASSESSMENT_ID,
                                request
                        )
                )
                .assertNext(response -> {
                    assertEquals(
                            ASSESSMENT_ID,
                            response.getAssessmentId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            response.getApplicationId()
                    );

                    assertEquals(
                            RiskAssessmentStatus.DECLINED.name(),
                            response.getStatus()
                    );

                    assertEquals(
                            "Risk level is not acceptable",
                            response.getRemarks()
                    );

                    assertEquals(
                            "ADMIN",
                            response.getAssessedBy()
                    );

                    assertFalse(
                            response.isNew()
                    );
                })
                .verifyComplete();

        verify(riskService)
                .declineRiskAssessment(
                        ASSESSMENT_ID,
                        request
                );
    }

    @Test
    void declineRiskAssessment_ShouldPropagateServiceError() {

        DeclineRiskRequest request =
                createDeclineRiskRequest();

        RuntimeException exception =
                new RuntimeException(
                        "Unable to decline risk assessment"
                );

        when(riskService.declineRiskAssessment(
                ASSESSMENT_ID,
                request
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        riskController.declineRiskAssessment(
                                ASSESSMENT_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && error.getMessage().equals(
                                "Unable to decline risk assessment"
                        )
                )
                .verify();

        verify(riskService)
                .declineRiskAssessment(
                        ASSESSMENT_ID,
                        request
                );
    }

    // =========================================================
    // EMPTY RESPONSE TESTS
    // =========================================================

    @Test
    void calculateRisk_ShouldCompleteEmpty_WhenServiceReturnsEmpty() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        when(riskService.calculateRisk(request))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        riskController.calculateRisk(request)
                )
                .verifyComplete();

        verify(riskService)
                .calculateRisk(request);
    }

    @Test
    void getRiskAssessment_ShouldCompleteEmpty_WhenServiceReturnsEmpty() {

        when(riskService.getRiskAssessment(
                APPLICATION_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        riskController.getRiskAssessment(
                                APPLICATION_ID
                        )
                )
                .verifyComplete();

        verify(riskService)
                .getRiskAssessment(
                        APPLICATION_ID
                );
    }

    @Test
    void approveRiskAssessment_ShouldCompleteEmpty_WhenServiceReturnsEmpty() {

        ApproveRiskRequest request =
                createApproveRiskRequest();

        when(riskService.approveRiskAssessment(
                ASSESSMENT_ID,
                request
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        riskController.approveRiskAssessment(
                                ASSESSMENT_ID,
                                request
                        )
                )
                .verifyComplete();

        verify(riskService)
                .approveRiskAssessment(
                        ASSESSMENT_ID,
                        request
                );
    }

    @Test
    void declineRiskAssessment_ShouldCompleteEmpty_WhenServiceReturnsEmpty() {

        DeclineRiskRequest request =
                createDeclineRiskRequest();

        when(riskService.declineRiskAssessment(
                ASSESSMENT_ID,
                request
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        riskController.declineRiskAssessment(
                                ASSESSMENT_ID,
                                request
                        )
                )
                .verifyComplete();

        verify(riskService)
                .declineRiskAssessment(
                        ASSESSMENT_ID,
                        request
                );
    }

    // =========================================================
    // TEST DATA HELPERS
    // =========================================================

    private RiskAssessmentRequest
    createRiskAssessmentRequest() {

        RiskAssessmentRequest request =
                new RiskAssessmentRequest();

        request.setApplicationId(
                APPLICATION_ID
        );

        request.setAssessedBy(
                "ADMIN"
        );

        request.setBuilderScore(
                80
        );

        request.setFinancialScore(
                70
        );

        request.setProjectProgressScore(
                90
        );

        request.setLocationScore(
                60
        );

        request.setApprovalScore(
                100
        );

        return request;
    }

    private ApproveRiskRequest
    createApproveRiskRequest() {

        ApproveRiskRequest request =
                new ApproveRiskRequest();

        request.setRemarks(
                "Risk assessment reviewed and approved"
        );

        request.setApprovedBy(
                "ADMIN"
        );

        return request;
    }

    private DeclineRiskRequest
    createDeclineRiskRequest() {

        DeclineRiskRequest request =
                new DeclineRiskRequest();

        request.setRemarks(
                "Risk level is not acceptable"
        );

        request.setDeclinedBy(
                "ADMIN"
        );

        return request;
    }

    private RiskAssessment createRiskAssessment(
            String status
    ) {
        RiskAssessment assessment =
                new RiskAssessment();

        assessment.setAssessmentId(
                ASSESSMENT_ID
        );

        assessment.setApplicationId(
                APPLICATION_ID
        );

        assessment.setRiskScore(
                80
        );

        assessment.setRiskCategory(
                RiskCategory.LOW.name()
        );

        assessment.setPremiumMultiplier(
                1.0
        );

        assessment.setRemarks(
                "Risk assessment calculated"
        );

        assessment.setStatus(
                status
        );

        assessment.setAssessedBy(
                "ADMIN"
        );

        assessment.setAssessedAt(
                LocalDateTime.of(
                        2026,
                        10,
                        4,
                        20,
                        30
                )
        );

        assessment.setNew(false);

        return assessment;
    }
}