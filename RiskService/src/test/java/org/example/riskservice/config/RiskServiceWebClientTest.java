package org.example.riskservice.config;

import org.example.riskservice.dto.ApproveRiskRequest;
import org.example.riskservice.dto.RiskAssessmentRequest;
import org.example.riskservice.entity.RiskAssessment;
import org.example.riskservice.enums.RiskAssessmentStatus;
import org.example.riskservice.enums.RiskCategory;
import org.example.riskservice.exceptions.ApplicationNotEligibleForRiskException;
import org.example.riskservice.exceptions.ApplicationNotFoundException;
import org.example.riskservice.exceptions.ApplicationServiceException;
import org.example.riskservice.exceptions.PolicyServiceException;
import org.example.riskservice.repository.RiskAssessmentRepository;
import org.example.riskservice.security.JwtService;
import org.example.riskservice.service.RiskServiceImpl;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskServiceWebClientTest {

    @Mock
    private ExchangeFunction exchangeFunction;

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @Mock
    private R2dbcEntityTemplate template;

    @Mock
    private ReactiveInsertOperation.ReactiveInsert<RiskAssessment>
            riskAssessmentInsert;

    @Mock
    private JwtService jwtService;

    private RiskServiceImpl riskService;

    private static final String APPLICATION_ID =
            "application-101";

    private static final String ASSESSMENT_ID =
            "assessment-101";

    private static final String SERVICE_TOKEN =
            "signed-internal-admin-token";

    @BeforeEach
    void setUp() {

        StepVerifier.setDefaultTimeout(
                Duration.ofSeconds(5)
        );

        WebClient webClient =
                WebClient.builder()
                        .exchangeFunction(exchangeFunction)
                        .build();

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
    // VERIFIED APPLICATION: INSERT NEW RISK ASSESSMENT
    // =========================================================

    @Test
    void calculateRisk_ShouldInsertAssessment_WhenApplicationIsVerified() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);



        when(riskAssessmentRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.empty()
        );

        when(template.insert(RiskAssessment.class))
                .thenReturn(riskAssessmentInsert);

        when(riskAssessmentInsert.using(
                any(RiskAssessment.class)
        )).thenAnswer(invocation -> {
            RiskAssessment assessment =
                    invocation.getArgument(0);

            return Mono.just(assessment);
        });

        AtomicReference<ClientRequest> capturedRequest =
                new AtomicReference<>();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {
            ClientRequest clientRequest =
                    invocation.getArgument(0);

            capturedRequest.set(clientRequest);

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            verifiedApplicationJson()
                    )
            );
        });

        StepVerifier.create(
                        riskService.calculateRisk(request)
                )
                .assertNext(assessment -> {
                    assertNotNull(
                            assessment.getAssessmentId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            assessment.getApplicationId()
                    );

                    assertEquals(
                            80,
                            assessment.getRiskScore()
                    );

                    assertEquals(
                            RiskCategory.LOW.name(),
                            assessment.getRiskCategory()
                    );

                    assertEquals(
                            1.0,
                            assessment.getPremiumMultiplier()
                    );

                    assertEquals(
                            RiskAssessmentStatus.PENDING.name(),
                            assessment.getStatus()
                    );

                    assertEquals(
                            "ADMIN",
                            assessment.getAssessedBy()
                    );

                    assertNotNull(
                            assessment.getAssessedAt()
                    );

                    assertTrue(
                            assessment.isNew()
                    );
                })
                .verifyComplete();

        ClientRequest applicationRequest =
                capturedRequest.get();

        assertNotNull(applicationRequest);

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

        verify(jwtService)
                .generateServiceToken();

        verify(riskAssessmentInsert)
                .using(any(RiskAssessment.class));
    }

    // =========================================================
    // VERIFIED APPLICATION: UPDATE EXISTING ASSESSMENT
    // =========================================================

    @Test
    void calculateRisk_ShouldUpdateAssessment_WhenPendingAssessmentExists() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        RiskAssessment existingAssessment =
                createRiskAssessment(
                        RiskAssessmentStatus.PENDING.name()
                );

        existingAssessment.setRiskScore(50);
        existingAssessment.setRiskCategory(
                RiskCategory.MEDIUM.name()
        );
        existingAssessment.setPremiumMultiplier(1.25);

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                verifiedApplicationJson()
                        )
                )
        );

        when(riskAssessmentRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(existingAssessment)
        );

        when(template.update(existingAssessment))
                .thenAnswer(invocation -> {
                    RiskAssessment assessment =
                            invocation.getArgument(0);

                    return Mono.just(assessment);
                });

        StepVerifier.create(
                        riskService.calculateRisk(request)
                )
                .assertNext(assessment -> {
                    assertEquals(
                            ASSESSMENT_ID,
                            assessment.getAssessmentId()
                    );

                    assertEquals(
                            80,
                            assessment.getRiskScore()
                    );

                    assertEquals(
                            RiskCategory.LOW.name(),
                            assessment.getRiskCategory()
                    );

                    assertEquals(
                            1.0,
                            assessment.getPremiumMultiplier()
                    );

                    assertEquals(
                            RiskAssessmentStatus.PENDING.name(),
                            assessment.getStatus()
                    );

                    assertFalse(
                            assessment.isNew()
                    );
                })
                .verifyComplete();

        verify(template)
                .update(existingAssessment);
    }

    // =========================================================
    // APPLICATION NOT FOUND
    // =========================================================

    @Test
    void calculateRisk_ShouldThrowApplicationNotFound_WhenApplicationIsMissing() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.NOT_FOUND,
                                """
                                {
                                  "code": "APPLICATION_NOT_FOUND",
                                  "message": "Application not found"
                                }
                                """
                        )
                )
        );

        StepVerifier.create(
                        riskService.calculateRisk(request)
                )
                .expectErrorMatches(error ->
                        error instanceof ApplicationNotFoundException
                                && error.getMessage().equals(
                                "Application not found: "
                                        + APPLICATION_ID
                        )
                )
                .verify();

        verify(riskAssessmentRepository, never())
                .findByApplicationId(any(String.class));

        verify(template, never())
                .insert(RiskAssessment.class);
    }

    // =========================================================
    // APPLICATION NOT VERIFIED
    // =========================================================

    @Test
    void calculateRisk_ShouldRejectApplication_WhenStatusIsSubmitted() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                """
                                {
                                  "applicationId": "application-101",
                                  "userId": 4,
                                  "status": "SUBMITTED"
                                }
                                """
                        )
                )
        );

        StepVerifier.create(
                        riskService.calculateRisk(request)
                )
                .expectErrorMatches(error ->
                        error instanceof
                                ApplicationNotEligibleForRiskException
                                && error.getMessage().equals(
                                "Application must be VERIFIED "
                                        + "before risk assessment. "
                                        + "Current status: SUBMITTED"
                        )
                )
                .verify();

        verify(riskAssessmentRepository, never())
                .findByApplicationId(any(String.class));

        verify(template, never())
                .insert(RiskAssessment.class);
    }

    // =========================================================
    // APPLICATION ID MISMATCH
    // =========================================================

    @Test
    void calculateRisk_ShouldThrowApplicationServiceException_WhenIdDoesNotMatch() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                """
                                {
                                  "applicationId": "different-application",
                                  "userId": 4,
                                  "status": "VERIFIED"
                                }
                                """
                        )
                )
        );

        StepVerifier.create(
                        riskService.calculateRisk(request)
                )
                .expectErrorMatches(error ->
                        error instanceof
                                ApplicationServiceException
                                && error.getMessage().equals(
                                "Application ID returned by "
                                        + "Application Service does not match"
                        )
                )
                .verify();

        verify(riskAssessmentRepository, never())
                .findByApplicationId(any(String.class));
    }

    // =========================================================
    // APPLICATION SERVICE UNAVAILABLE
    // =========================================================

    @Test
    void calculateRisk_ShouldMapApplicationServiceConnectionFailure() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.error(
                        new RuntimeException(
                                "Connection refused"
                        )
                )
        );

        StepVerifier.create(
                        riskService.calculateRisk(request)
                )
                .expectErrorMatches(error ->
                        error instanceof
                                ApplicationServiceException
                                && error.getMessage().equals(
                                "Unable to validate application "
                                        + "with Application Service"
                        )
                )
                .verify();

        verify(riskAssessmentRepository, never())
                .findByApplicationId(any(String.class));
    }

    // =========================================================
    // APPROVAL SUCCESS AND POLICY SERVICE CALL
    // =========================================================

    @Test
    void approveRiskAssessment_ShouldGenerateQuoteSuccessfully() {

        ApproveRiskRequest request =
                createApproveRequest();

        RiskAssessment pendingAssessment =
                createRiskAssessment(
                        RiskAssessmentStatus.PENDING.name()
                );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(riskAssessmentRepository.findById(
                ASSESSMENT_ID
        )).thenReturn(
                Mono.just(pendingAssessment)
        );

        when(template.update(pendingAssessment))
                .thenAnswer(invocation -> {
                    RiskAssessment assessment =
                            invocation.getArgument(0);

                    return Mono.just(assessment);
                });

        AtomicReference<ClientRequest> capturedRequest =
                new AtomicReference<>();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {
            ClientRequest clientRequest =
                    invocation.getArgument(0);

            capturedRequest.set(clientRequest);

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            """
                            {
                              "quoteId": "quote-101",
                              "applicationId": "application-101",
                              "status": "GENERATED"
                            }
                            """
                    )
            );
        });

        StepVerifier.create(
                        riskService.approveRiskAssessment(
                                ASSESSMENT_ID,
                                request
                        )
                )
                .assertNext(assessment -> {
                    assertEquals(
                            ASSESSMENT_ID,
                            assessment.getAssessmentId()
                    );

                    assertEquals(
                            RiskAssessmentStatus.APPROVED.name(),
                            assessment.getStatus()
                    );

                    assertEquals(
                            request.getRemarks(),
                            assessment.getRemarks()
                    );

                    assertEquals(
                            request.getApprovedBy(),
                            assessment.getAssessedBy()
                    );

                    assertNotNull(
                            assessment.getAssessedAt()
                    );

                    assertFalse(
                            assessment.isNew()
                    );
                })
                .verifyComplete();

        ClientRequest policyRequest =
                capturedRequest.get();

        assertNotNull(policyRequest);

        assertEquals(
                HttpMethod.POST,
                policyRequest.method()
        );

        assertEquals(
                "http://localhost:8085/api/v1/quotes/applications/"
                        + APPLICATION_ID,
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

        verify(template)
                .update(pendingAssessment);

        verify(jwtService)
                .generateServiceToken();
    }

    // =========================================================
    // POLICY SERVICE FAILURE
    // =========================================================

    @Test
    void approveRiskAssessment_ShouldMapPolicyServiceFailure() {

        ApproveRiskRequest request =
                createApproveRequest();

        RiskAssessment pendingAssessment =
                createRiskAssessment(
                        RiskAssessmentStatus.PENDING.name()
                );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(riskAssessmentRepository.findById(
                ASSESSMENT_ID
        )).thenReturn(
                Mono.just(pendingAssessment)
        );

        when(template.update(pendingAssessment))
                .thenReturn(
                        Mono.just(pendingAssessment)
                );

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.error(
                        new RuntimeException(
                                "Policy Service unavailable"
                        )
                )
        );

        StepVerifier.create(
                        riskService.approveRiskAssessment(
                                ASSESSMENT_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PolicyServiceException
                                && error.getMessage().equals(
                                "Unable to generate quote"
                        )
                )
                .verify();

        verify(template)
                .update(pendingAssessment);
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

    private String verifiedApplicationJson() {
        return """
                {
                  "applicationId": "application-101",
                  "userId": 4,
                  "status": "VERIFIED"
                }
                """;
    }

    private RiskAssessmentRequest createRiskAssessmentRequest() {

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

    private ApproveRiskRequest createApproveRequest() {

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