package org.example.riskservice.service;

import org.example.riskservice.dto.ApproveRiskRequest;
import org.example.riskservice.dto.DeclineRiskRequest;
import org.example.riskservice.dto.RiskAssessmentRequest;
import org.example.riskservice.entity.RiskAssessment;
import org.example.riskservice.enums.RiskAssessmentStatus;
import org.example.riskservice.enums.RiskCategory;
import org.example.riskservice.exceptions.InvalidRiskAssessmentRequestException;
import org.example.riskservice.exceptions.RiskAssessmentAlreadyFinalizedException;
import org.example.riskservice.exceptions.RiskAssessmentNotFoundException;
import org.example.riskservice.repository.RiskAssessmentRepository;
import org.example.riskservice.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskServiceImplTest {

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @Mock
    private WebClient webClient;

    @Mock
    private R2dbcEntityTemplate template;

    @Mock
    private JwtService jwtService;

    private RiskServiceImpl riskService;

    private static final String APPLICATION_ID =
            "application-101";

    private static final String ASSESSMENT_ID =
            "assessment-101";

    @BeforeEach
    void setUp() {

        StepVerifier.setDefaultTimeout(
                Duration.ofSeconds(5)
        );

        riskService =
                new RiskServiceImpl(
                        riskAssessmentRepository,
                        webClient,
                        template,
                        jwtService
                );

        ReflectionTestUtils.setField(
                riskService,
                "applicationServiceUrl",
                "http://localhost:8082"
        );

        ReflectionTestUtils.setField(
                riskService,
                "policyServiceUrl",
                "http://localhost:8085"
        );
    }

    @AfterEach
    void tearDown() {
        StepVerifier.resetDefaultTimeout();
    }

    // =========================================================
    // CALCULATE RISK VALIDATION
    // =========================================================

    @Test
    void calculateRisk_ShouldThrowException_WhenRequestIsNull() {

        StepVerifier.create(
                        Mono.defer(() ->
                                riskService.calculateRisk(null)
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidRiskAssessmentRequestException
                                && error.getMessage().equals(
                                "Risk assessment request is required"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void calculateRisk_ShouldThrowException_WhenApplicationIdIsNull() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        request.setApplicationId(null);

        StepVerifier.create(
                        Mono.defer(() ->
                                riskService.calculateRisk(request)
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidRiskAssessmentRequestException
                                && error.getMessage().equals(
                                "Application ID is required"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void calculateRisk_ShouldThrowException_WhenApplicationIdIsBlank() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        request.setApplicationId("   ");

        StepVerifier.create(
                        Mono.defer(() ->
                                riskService.calculateRisk(request)
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidRiskAssessmentRequestException
                                && error.getMessage().equals(
                                "Application ID is required"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void calculateRisk_ShouldThrowException_WhenAssessedByIsBlank() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        request.setAssessedBy("");

        StepVerifier.create(
                        Mono.defer(() ->
                                riskService.calculateRisk(request)
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidRiskAssessmentRequestException
                                && error.getMessage().equals(
                                "Assessed by is required"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void calculateRisk_ShouldThrowException_WhenBuilderScoreIsNegative() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        request.setBuilderScore(-1);

        StepVerifier.create(
                        Mono.defer(() ->
                                riskService.calculateRisk(request)
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidRiskAssessmentRequestException
                                && error.getMessage().equals(
                                "Builder score must be between 0 and 100"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void calculateRisk_ShouldThrowException_WhenFinancialScoreExceeds100() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        request.setFinancialScore(101);

        StepVerifier.create(
                        Mono.defer(() ->
                                riskService.calculateRisk(request)
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidRiskAssessmentRequestException
                                && error.getMessage().equals(
                                "Financial score must be between 0 and 100"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void calculateRisk_ShouldThrowException_WhenProgressScoreExceeds100() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        request.setProjectProgressScore(101);

        StepVerifier.create(
                        Mono.defer(() ->
                                riskService.calculateRisk(request)
                        )
                )
                .expectError(
                        InvalidRiskAssessmentRequestException.class
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void calculateRisk_ShouldThrowException_WhenLocationScoreIsNegative() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        request.setLocationScore(-5);

        StepVerifier.create(
                        Mono.defer(() ->
                                riskService.calculateRisk(request)
                        )
                )
                .expectError(
                        InvalidRiskAssessmentRequestException.class
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void calculateRisk_ShouldThrowException_WhenApprovalScoreExceeds100() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        request.setApprovalScore(110);

        StepVerifier.create(
                        Mono.defer(() ->
                                riskService.calculateRisk(request)
                        )
                )
                .expectError(
                        InvalidRiskAssessmentRequestException.class
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    // =========================================================
    // GET RISK ASSESSMENT
    // =========================================================

    @Test
    void getRiskAssessment_ShouldReturnAssessment_WhenFound() {

        RiskAssessment assessment =
                createRiskAssessment(
                        RiskAssessmentStatus.PENDING.name()
                );

        when(riskAssessmentRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(assessment)
        );

        StepVerifier.create(
                        riskService.getRiskAssessment(
                                APPLICATION_ID
                        )
                )
                .assertNext(result -> {
                    assertEquals(
                            ASSESSMENT_ID,
                            result.getAssessmentId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            result.getApplicationId()
                    );

                    assertEquals(
                            RiskAssessmentStatus.PENDING.name(),
                            result.getStatus()
                    );

                    assertEquals(
                            80,
                            result.getRiskScore()
                    );
                })
                .verifyComplete();

        verify(riskAssessmentRepository)
                .findByApplicationId(APPLICATION_ID);
    }

    @Test
    void getRiskAssessment_ShouldThrowNotFound_WhenMissing() {

        when(riskAssessmentRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        riskService.getRiskAssessment(
                                APPLICATION_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                RiskAssessmentNotFoundException
                                && error.getMessage().equals(
                                "No risk assessment found for application: "
                                        + APPLICATION_ID
                        )
                )
                .verify();
    }

    @Test
    void getRiskAssessment_ShouldThrowException_WhenApplicationIdIsBlank() {

        StepVerifier.create(
                        Mono.defer(() ->
                                riskService.getRiskAssessment(" ")
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidRiskAssessmentRequestException
                                && error.getMessage().equals(
                                "Application ID is required"
                        )
                )
                .verify();

        verify(riskAssessmentRepository, never())
                .findByApplicationId(any());
    }

    // =========================================================
    // DECLINE RISK ASSESSMENT
    // =========================================================

    @Test
    void declineRiskAssessment_ShouldUpdatePendingAssessment() {

        DeclineRiskRequest request =
                createDeclineRequest();

        RiskAssessment assessment =
                createRiskAssessment(
                        RiskAssessmentStatus.PENDING.name()
                );

        when(riskAssessmentRepository.findById(
                ASSESSMENT_ID
        )).thenReturn(
                Mono.just(assessment)
        );

        when(template.update(assessment))
                .thenReturn(
                        Mono.just(assessment)
                );

        StepVerifier.create(
                        riskService.declineRiskAssessment(
                                ASSESSMENT_ID,
                                request
                        )
                )
                .assertNext(result -> {
                    assertEquals(
                            RiskAssessmentStatus.DECLINED.name(),
                            result.getStatus()
                    );

                    assertEquals(
                            request.getRemarks(),
                            result.getRemarks()
                    );

                    assertEquals(
                            request.getDeclinedBy(),
                            result.getAssessedBy()
                    );

                    assertNotNull(
                            result.getAssessedAt()
                    );

                    assertFalse(
                            result.isNew()
                    );
                })
                .verifyComplete();

        verify(template)
                .update(assessment);
    }

    @Test
    void declineRiskAssessment_ShouldThrowNotFound_WhenAssessmentMissing() {

        DeclineRiskRequest request =
                createDeclineRequest();

        when(riskAssessmentRepository.findById(
                ASSESSMENT_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        riskService.declineRiskAssessment(
                                ASSESSMENT_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                RiskAssessmentNotFoundException
                                && error.getMessage().equals(
                                "Risk assessment not found: "
                                        + ASSESSMENT_ID
                        )
                )
                .verify();

        verify(template, never())
                .update(any(RiskAssessment.class));
    }

    @Test
    void declineRiskAssessment_ShouldThrowException_WhenAlreadyApproved() {

        DeclineRiskRequest request =
                createDeclineRequest();

        RiskAssessment assessment =
                createRiskAssessment(
                        RiskAssessmentStatus.APPROVED.name()
                );

        when(riskAssessmentRepository.findById(
                ASSESSMENT_ID
        )).thenReturn(
                Mono.just(assessment)
        );

        StepVerifier.create(
                        riskService.declineRiskAssessment(
                                ASSESSMENT_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                RiskAssessmentAlreadyFinalizedException
                                && error.getMessage().equals(
                                "Risk assessment is already finalized as APPROVED"
                        )
                )
                .verify();

        verify(template, never())
                .update(any(RiskAssessment.class));
    }

    @Test
    void declineRiskAssessment_ShouldThrowException_WhenAlreadyDeclined() {

        DeclineRiskRequest request =
                createDeclineRequest();

        RiskAssessment assessment =
                createRiskAssessment(
                        RiskAssessmentStatus.DECLINED.name()
                );

        when(riskAssessmentRepository.findById(
                ASSESSMENT_ID
        )).thenReturn(
                Mono.just(assessment)
        );

        StepVerifier.create(
                        riskService.declineRiskAssessment(
                                ASSESSMENT_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                RiskAssessmentAlreadyFinalizedException
                                && error.getMessage().equals(
                                "Risk assessment is already finalized as DECLINED"
                        )
                )
                .verify();

        verify(template, never())
                .update(any(RiskAssessment.class));
    }

    @Test
    void declineRiskAssessment_ShouldThrowException_WhenRequestIsNull() {

        StepVerifier.create(
                        Mono.defer(() ->
                                riskService.declineRiskAssessment(
                                        ASSESSMENT_ID,
                                        null
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidRiskAssessmentRequestException
                                && error.getMessage().equals(
                                "Decline request is required"
                        )
                )
                .verify();

        verify(riskAssessmentRepository, never())
                .findById(any(String.class));
    }

    @Test
    void declineRiskAssessment_ShouldThrowException_WhenRemarksAreBlank() {

        DeclineRiskRequest request =
                createDeclineRequest();

        request.setRemarks("");

        StepVerifier.create(
                        Mono.defer(() ->
                                riskService.declineRiskAssessment(
                                        ASSESSMENT_ID,
                                        request
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidRiskAssessmentRequestException
                                && error.getMessage().equals(
                                "Remarks are required"
                        )
                )
                .verify();

        verify(riskAssessmentRepository, never())
                .findById(any(String.class));
    }

    @Test
    void declineRiskAssessment_ShouldThrowException_WhenDeclinedByIsBlank() {

        DeclineRiskRequest request =
                createDeclineRequest();

        request.setDeclinedBy("   ");

        StepVerifier.create(
                        Mono.defer(() ->
                                riskService.declineRiskAssessment(
                                        ASSESSMENT_ID,
                                        request
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidRiskAssessmentRequestException
                                && error.getMessage().equals(
                                "Declined by is required"
                        )
                )
                .verify();

        verify(riskAssessmentRepository, never())
                .findById(any(String.class));
    }

    // =========================================================
    // APPROVAL VALIDATION AND FINAL-STATE PROTECTION
    // =========================================================

    @Test
    void approveRiskAssessment_ShouldThrowException_WhenRequestIsNull() {

        StepVerifier.create(
                        Mono.defer(() ->
                                riskService.approveRiskAssessment(
                                        ASSESSMENT_ID,
                                        null
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidRiskAssessmentRequestException
                                && error.getMessage().equals(
                                "Approval request is required"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void approveRiskAssessment_ShouldThrowException_WhenRemarksAreBlank() {

        ApproveRiskRequest request =
                createApproveRequest();

        request.setRemarks("");

        StepVerifier.create(
                        Mono.defer(() ->
                                riskService.approveRiskAssessment(
                                        ASSESSMENT_ID,
                                        request
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidRiskAssessmentRequestException
                                && error.getMessage().equals(
                                "Remarks are required"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void approveRiskAssessment_ShouldThrowException_WhenApprovedByIsBlank() {

        ApproveRiskRequest request =
                createApproveRequest();

        request.setApprovedBy(" ");

        StepVerifier.create(
                        Mono.defer(() ->
                                riskService.approveRiskAssessment(
                                        ASSESSMENT_ID,
                                        request
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidRiskAssessmentRequestException
                                && error.getMessage().equals(
                                "Approved by is required"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void approveRiskAssessment_ShouldThrowNotFound_WhenAssessmentMissing() {

        ApproveRiskRequest request =
                createApproveRequest();

        when(jwtService.generateServiceToken())
                .thenReturn("service-token");

        when(riskAssessmentRepository.findById(
                ASSESSMENT_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        riskService.approveRiskAssessment(
                                ASSESSMENT_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                RiskAssessmentNotFoundException
                                && error.getMessage().equals(
                                "Risk assessment not found: "
                                        + ASSESSMENT_ID
                        )
                )
                .verify();

        verify(template, never())
                .update(any(RiskAssessment.class));
    }

    @Test
    void approveRiskAssessment_ShouldThrowException_WhenAlreadyApproved() {

        ApproveRiskRequest request =
                createApproveRequest();

        RiskAssessment assessment =
                createRiskAssessment(
                        RiskAssessmentStatus.APPROVED.name()
                );

        when(jwtService.generateServiceToken())
                .thenReturn("service-token");

        when(riskAssessmentRepository.findById(
                ASSESSMENT_ID
        )).thenReturn(
                Mono.just(assessment)
        );

        StepVerifier.create(
                        riskService.approveRiskAssessment(
                                ASSESSMENT_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                RiskAssessmentAlreadyFinalizedException
                                && error.getMessage().equals(
                                "Risk assessment is already finalized as APPROVED"
                        )
                )
                .verify();

        verify(template, never())
                .update(any(RiskAssessment.class));
    }

    @Test
    void approveRiskAssessment_ShouldThrowException_WhenAlreadyDeclined() {

        ApproveRiskRequest request =
                createApproveRequest();

        RiskAssessment assessment =
                createRiskAssessment(
                        RiskAssessmentStatus.DECLINED.name()
                );

        when(jwtService.generateServiceToken())
                .thenReturn("service-token");

        when(riskAssessmentRepository.findById(
                ASSESSMENT_ID
        )).thenReturn(
                Mono.just(assessment)
        );

        StepVerifier.create(
                        riskService.approveRiskAssessment(
                                ASSESSMENT_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                RiskAssessmentAlreadyFinalizedException
                                && error.getMessage().equals(
                                "Risk assessment is already finalized as DECLINED"
                        )
                )
                .verify();

        verify(template, never())
                .update(any(RiskAssessment.class));
    }

    // =========================================================
    // TEST HELPERS
    // =========================================================

    private RiskAssessmentRequest createRiskAssessmentRequest() {

        RiskAssessmentRequest request =
                new RiskAssessmentRequest();

        request.setApplicationId(APPLICATION_ID);
        request.setAssessedBy("ADMIN");
        request.setBuilderScore(80);
        request.setFinancialScore(70);
        request.setProjectProgressScore(90);
        request.setLocationScore(60);
        request.setApprovalScore(100);

        return request;
    }

    private ApproveRiskRequest createApproveRequest() {

        ApproveRiskRequest request =
                new ApproveRiskRequest();

        request.setRemarks(
                "Risk assessment reviewed and approved"
        );

        request.setApprovedBy("ADMIN");

        return request;
    }

    private DeclineRiskRequest createDeclineRequest() {

        DeclineRiskRequest request =
                new DeclineRiskRequest();

        request.setRemarks(
                "Risk level is not acceptable"
        );

        request.setDeclinedBy("ADMIN");

        return request;
    }

    private RiskAssessment createRiskAssessment(
            String status
    ) {
        RiskAssessment assessment =
                new RiskAssessment();

        assessment.setAssessmentId(ASSESSMENT_ID);
        assessment.setApplicationId(APPLICATION_ID);
        assessment.setRiskScore(80);
        assessment.setRiskCategory(
                RiskCategory.LOW.name()
        );
        assessment.setPremiumMultiplier(1.0);
        assessment.setRemarks(
                "Risk assessment calculated"
        );
        assessment.setStatus(status);
        assessment.setAssessedBy("ADMIN");
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