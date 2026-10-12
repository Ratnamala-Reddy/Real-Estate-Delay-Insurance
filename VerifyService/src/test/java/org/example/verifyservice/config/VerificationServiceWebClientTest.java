package org.example.verifyservice.config;

import org.example.verifyservice.dto.ApprovalRequest;
import org.example.verifyservice.dto.RejectVerificationRequest;
import org.example.verifyservice.entity.VerificationRecord;
import org.example.verifyservice.enums.VerificationStatus;
import org.example.verifyservice.exception.ApplicationServiceException;
import org.example.verifyservice.exception.RiskServiceException;
import org.example.verifyservice.repository.VerificationRepository;
import org.example.verifyservice.security.JwtService;
import org.example.verifyservice.service.VerificationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationServiceWebClientTest {

    @Mock
    private ExchangeFunction exchangeFunction;

    @Mock
    private VerificationRepository verificationRepository;

    @Mock
    private R2dbcEntityTemplate template;

    @Mock
    private JwtService jwtService;

    private VerificationServiceImpl verificationService;

    private static final String APPLICATION_ID =
            "application-101";

    private static final String VERIFICATION_ID =
            "verification-101";

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

        verificationService =
                new VerificationServiceImpl(
                        webClient,
                        verificationRepository,
                        template,
                        jwtService
                );

        ReflectionTestUtils.setField(
                verificationService,
                "applicationServiceUrl",
                "http://localhost:8082"
        );

        ReflectionTestUtils.setField(
                verificationService,
                "riskServiceUrl",
                "http://localhost:8084"
        );
    }

    @AfterEach
    void tearDown() {
        StepVerifier.resetDefaultTimeout();
    }

    // =========================================================
    // VERIFICATION QUEUE SUCCESS
    // =========================================================

    @Test
    void getVerificationQueue_ShouldReturnOnlySubmittedApplications() {

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        String responseBody =
                """
                [
                  {
                    "applicationId": "application-101",
                    "userId": 4,
                    "builderName": "Apexes Infrastructure",
                    "projectName": "Skyline Heights",
                    "unitNumber": "B-1404",
                    "propertyValue": 12500000.00,
                    "purchaseDate": "2024-11-10",
                    "promisedPossessionDate": "2026-12-31",
                    "status": "SUBMITTED"
                  },
                  {
                    "applicationId": "application-102",
                    "userId": 5,
                    "builderName": "ABC Builders",
                    "projectName": "Green Residency",
                    "unitNumber": "A-101",
                    "propertyValue": 8500000.00,
                    "purchaseDate": "2024-10-10",
                    "promisedPossessionDate": "2026-10-31",
                    "status": "DRAFT"
                  }
                ]
                """;

        ClientResponse clientResponse =
                jsonResponse(
                        HttpStatus.OK,
                        responseBody
                );

        AtomicReference<ClientRequest> capturedRequest =
                new AtomicReference<>();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {
            ClientRequest request =
                    invocation.getArgument(0);

            capturedRequest.set(request);

            return Mono.just(clientResponse);
        });

        StepVerifier.create(
                        verificationService
                                .getVerificationQueue()
                )
                .assertNext(application -> {
                    assertEquals(
                            "application-101",
                            application.getApplicationId()
                    );

                    assertEquals(
                            "SUBMITTED",
                            application.getStatus()
                    );
                })
                .verifyComplete();

        ClientRequest request =
                capturedRequest.get();

        assertNotNull(request);

        assertEquals(
                HttpMethod.GET,
                request.method()
        );

        assertEquals(
                "http://localhost:8082/api/v1/applications/all",
                request.url().toString()
        );

        assertEquals(
                "Bearer " + SERVICE_TOKEN,
                request.headers().getFirst(
                        HttpHeaders.AUTHORIZATION
                )
        );

        verify(jwtService)
                .generateServiceToken();
    }

    // =========================================================
    // VERIFICATION QUEUE FAILURE
    // =========================================================

    @Test
    void getVerificationQueue_ShouldMapApplicationServiceFailure() {

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
                        verificationService
                                .getVerificationQueue()
                )
                .expectErrorMatches(error ->
                        error instanceof ApplicationServiceException
                                && error.getMessage().equals(
                                "Unable to fetch verification queue from Application Service"
                        )
                )
                .verify();
    }

    // =========================================================
    // APPROVAL SUCCESS
    // =========================================================

    @Test
    void approveVerification_ShouldUpdateApplicationAndCreateRiskAssessment() {

        ApprovalRequest request =
                createApprovalRequest();

        VerificationRecord existingRecord =
                createVerificationRecord(
                        VerificationStatus.PROPERTY_VERIFIED.name()
                );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(verificationRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(existingRecord)
        );

        when(template.update(existingRecord))
                .thenReturn(
                        Mono.just(existingRecord)
                );

        ClientResponse applicationResponse =
                jsonResponse(
                        HttpStatus.OK,
                        """
                        {
                          "applicationId": "application-101",
                          "status": "VERIFIED"
                        }
                        """
                );

        ClientResponse riskResponse =
                jsonResponse(
                        HttpStatus.OK,
                        """
                        {
                          "riskAssessmentId": "risk-101",
                          "applicationId": "application-101"
                        }
                        """
                );

        AtomicInteger callNumber =
                new AtomicInteger();

        AtomicReference<ClientRequest> applicationRequest =
                new AtomicReference<>();

        AtomicReference<ClientRequest> riskRequest =
                new AtomicReference<>();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            ClientRequest clientRequest =
                    invocation.getArgument(0);

            int currentCall =
                    callNumber.incrementAndGet();

            if (currentCall == 1) {
                applicationRequest.set(clientRequest);

                return Mono.just(
                        applicationResponse
                );
            }

            riskRequest.set(clientRequest);

            return Mono.just(
                    riskResponse
            );
        });

        StepVerifier.create(
                        verificationService.approveVerification(
                                APPLICATION_ID,
                                request
                        )
                )
                .assertNext(record -> {
                    assertEquals(
                            APPLICATION_ID,
                            record.getApplicationId()
                    );

                    assertEquals(
                            VerificationStatus.VERIFIED.name(),
                            record.getVerificationStatus()
                    );

                    assertEquals(
                            request.getRemarks(),
                            record.getRemarks()
                    );

                    assertEquals(
                            request.getApprovedBy(),
                            record.getVerifiedBy()
                    );
                })
                .verifyComplete();

        assertEquals(
                2,
                callNumber.get()
        );

        assertNotNull(
                applicationRequest.get()
        );

        assertEquals(
                HttpMethod.PATCH,
                applicationRequest.get().method()
        );

        assertEquals(
                "http://localhost:8082/api/v1/applications/"
                        + APPLICATION_ID
                        + "/status",
                applicationRequest.get()
                        .url()
                        .toString()
        );

        assertEquals(
                "Bearer " + SERVICE_TOKEN,
                applicationRequest.get()
                        .headers()
                        .getFirst(
                                HttpHeaders.AUTHORIZATION
                        )
        );

        assertNotNull(
                riskRequest.get()
        );

        assertEquals(
                HttpMethod.POST,
                riskRequest.get().method()
        );

        assertEquals(
                "http://localhost:8084/api/v1/risk-assessments",
                riskRequest.get()
                        .url()
                        .toString()
        );

        assertEquals(
                "Bearer " + SERVICE_TOKEN,
                riskRequest.get()
                        .headers()
                        .getFirst(
                                HttpHeaders.AUTHORIZATION
                        )
        );

        verify(template)
                .update(existingRecord);

        verify(jwtService)
                .generateServiceToken();
    }

    // =========================================================
    // APPROVAL APPLICATION SERVICE FAILURE
    // =========================================================

    @Test
    void approveVerification_ShouldMapApplicationServiceFailure() {

        ApprovalRequest request =
                createApprovalRequest();

        VerificationRecord existingRecord =
                createVerificationRecord(
                        VerificationStatus.PROPERTY_VERIFIED.name()
                );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(verificationRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(existingRecord)
        );

        when(template.update(existingRecord))
                .thenReturn(
                        Mono.just(existingRecord)
                );

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.error(
                        new RuntimeException(
                                "Application Service unavailable"
                        )
                )
        );

        StepVerifier.create(
                        verificationService.approveVerification(
                                APPLICATION_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ApplicationServiceException
                                && error.getMessage().equals(
                                "Unable to update application status to VERIFIED"
                        )
                )
                .verify();
    }

    // =========================================================
    // APPROVAL RISK SERVICE FAILURE
    // =========================================================

    @Test
    void approveVerification_ShouldMapRiskServiceFailure() {

        ApprovalRequest request =
                createApprovalRequest();

        VerificationRecord existingRecord =
                createVerificationRecord(
                        VerificationStatus.PROPERTY_VERIFIED.name()
                );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(verificationRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(existingRecord)
        );

        when(template.update(existingRecord))
                .thenReturn(
                        Mono.just(existingRecord)
                );

        ClientResponse applicationResponse =
                jsonResponse(
                        HttpStatus.OK,
                        """
                        {
                          "applicationId": "application-101",
                          "status": "VERIFIED"
                        }
                        """
                );

        AtomicInteger callNumber =
                new AtomicInteger();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            if (callNumber.incrementAndGet() == 1) {
                return Mono.just(
                        applicationResponse
                );
            }

            return Mono.error(
                    new RuntimeException(
                            "Risk Service unavailable"
                    )
            );
        });

        StepVerifier.create(
                        verificationService.approveVerification(
                                APPLICATION_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof RiskServiceException
                                && error.getMessage().equals(
                                "Unable to create risk assessment"
                        )
                )
                .verify();

        assertEquals(
                2,
                callNumber.get()
        );
    }

    // =========================================================
    // REJECTION SUCCESS
    // =========================================================

    @Test
    void rejectApplication_ShouldUpdateApplicationStatus() {

        RejectVerificationRequest request =
                createRejectRequest();

        VerificationRecord existingRecord =
                createVerificationRecord(
                        VerificationStatus.PROPERTY_VERIFIED.name()
                );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(verificationRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(existingRecord)
        );

        when(template.update(existingRecord))
                .thenReturn(
                        Mono.just(existingRecord)
                );

        ClientResponse applicationResponse =
                jsonResponse(
                        HttpStatus.OK,
                        """
                        {
                          "applicationId": "application-101",
                          "status": "REJECTED"
                        }
                        """
                );

        AtomicReference<ClientRequest> capturedRequest =
                new AtomicReference<>();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            ClientRequest clientRequest =
                    invocation.getArgument(0);

            capturedRequest.set(clientRequest);

            return Mono.just(
                    applicationResponse
            );
        });

        StepVerifier.create(
                        verificationService.rejectApplication(
                                APPLICATION_ID,
                                request
                        )
                )
                .assertNext(record -> {
                    assertEquals(
                            VerificationStatus.REJECTED.name(),
                            record.getVerificationStatus()
                    );

                    assertEquals(
                            request.getRemarks(),
                            record.getRemarks()
                    );

                    assertEquals(
                            request.getRejectedBy(),
                            record.getVerifiedBy()
                    );
                })
                .verifyComplete();

        ClientRequest clientRequest =
                capturedRequest.get();

        assertNotNull(clientRequest);

        assertEquals(
                HttpMethod.PATCH,
                clientRequest.method()
        );

        assertEquals(
                "http://localhost:8082/api/v1/applications/"
                        + APPLICATION_ID
                        + "/status",
                clientRequest.url().toString()
        );

        assertEquals(
                "Bearer " + SERVICE_TOKEN,
                clientRequest.headers().getFirst(
                        HttpHeaders.AUTHORIZATION
                )
        );
    }

    // =========================================================
    // REJECTION APPLICATION SERVICE FAILURE
    // =========================================================

    @Test
    void rejectApplication_ShouldMapApplicationServiceFailure() {

        RejectVerificationRequest request =
                createRejectRequest();

        VerificationRecord existingRecord =
                createVerificationRecord(
                        VerificationStatus.PROPERTY_VERIFIED.name()
                );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(verificationRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(existingRecord)
        );

        when(template.update(existingRecord))
                .thenReturn(
                        Mono.just(existingRecord)
                );

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.error(
                        new RuntimeException(
                                "Application Service unavailable"
                        )
                )
        );

        StepVerifier.create(
                        verificationService.rejectApplication(
                                APPLICATION_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ApplicationServiceException
                                && error.getMessage().equals(
                                "Unable to update application status to REJECTED"
                        )
                )
                .verify();
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

    private ApprovalRequest createApprovalRequest() {

        ApprovalRequest request =
                new ApprovalRequest();

        request.setRemarks(
                "All verification checks completed"
        );

        request.setApprovedBy(
                "ADMIN"
        );

        return request;
    }

    private RejectVerificationRequest createRejectRequest() {

        RejectVerificationRequest request =
                new RejectVerificationRequest();

        request.setRemarks(
                "Verification rejected"
        );

        request.setRejectedBy(
                "ADMIN"
        );

        return request;
    }

    private VerificationRecord createVerificationRecord(
            String status
    ) {
        VerificationRecord record =
                new VerificationRecord();

        record.setVerificationId(
                VERIFICATION_ID
        );

        record.setApplicationId(
                APPLICATION_ID
        );

        record.setVerificationStatus(
                status
        );

        record.setRemarks(
                "Existing verification remarks"
        );

        record.setVerifiedBy(
                "ADMIN"
        );

        record.setVerifiedAt(
                LocalDateTime.of(
                        2026,
                        10,
                        4,
                        18,
                        30
                )
        );

        record.setNew(false);

        return record;
    }
}