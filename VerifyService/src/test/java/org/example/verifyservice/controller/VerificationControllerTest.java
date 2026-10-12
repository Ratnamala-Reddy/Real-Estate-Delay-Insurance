package org.example.verifyservice.controller;

import org.example.verifyservice.dto.AdditionalInfoRequest;
import org.example.verifyservice.dto.ApplicationResponse;
import org.example.verifyservice.dto.ApprovalRequest;
import org.example.verifyservice.dto.PropertyVerificationRequest;
import org.example.verifyservice.dto.RejectVerificationRequest;
import org.example.verifyservice.entity.VerificationRecord;
import org.example.verifyservice.enums.VerificationStatus;
import org.example.verifyservice.service.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationControllerTest {

    @Mock
    private VerificationService verificationService;

    private VerificationController verificationController;

    private static final String APPLICATION_ID =
            "application-101";

    private static final String VERIFICATION_ID =
            "verification-101";

    @BeforeEach
    void setUp() {
        verificationController =
                new VerificationController(
                        verificationService
                );
    }

    // =========================================================
    // GET VERIFICATION QUEUE TESTS
    // =========================================================

    @Test
    void getVerificationQueue_ShouldReturnSubmittedApplications() {

        ApplicationResponse first =
                createApplicationResponse(
                        APPLICATION_ID,
                        4,
                        "SUBMITTED"
                );

        ApplicationResponse second =
                createApplicationResponse(
                        "application-102",
                        5,
                        "SUBMITTED"
                );

        when(verificationService.getVerificationQueue())
                .thenReturn(
                        Flux.just(first, second)
                );

        StepVerifier.create(
                        verificationController
                                .getVerificationQueue()
                )
                .expectNext(first)
                .expectNext(second)
                .verifyComplete();

        verify(verificationService)
                .getVerificationQueue();
    }

    @Test
    void getVerificationQueue_ShouldReturnEmptyFlux_WhenQueueIsEmpty() {

        when(verificationService.getVerificationQueue())
                .thenReturn(Flux.empty());

        StepVerifier.create(
                        verificationController
                                .getVerificationQueue()
                )
                .verifyComplete();

        verify(verificationService)
                .getVerificationQueue();
    }

    @Test
    void getVerificationQueue_ShouldPropagateServiceError() {

        RuntimeException exception =
                new RuntimeException(
                        "Application Service unavailable"
                );

        when(verificationService.getVerificationQueue())
                .thenReturn(
                        Flux.error(exception)
                );

        StepVerifier.create(
                        verificationController
                                .getVerificationQueue()
                )
                .expectErrorMatches(error ->
                        error == exception
                                && error.getMessage().equals(
                                "Application Service unavailable"
                        )
                )
                .verify();
    }

    // =========================================================
    // PROPERTY VERIFICATION TESTS
    // =========================================================

    @Test
    void verifyProperty_ShouldReturnPropertyVerifiedRecord() {

        PropertyVerificationRequest request =
                createPropertyVerificationRequest();

        VerificationRecord verificationRecord =
                createVerificationRecord(
                        VerificationStatus
                                .PROPERTY_VERIFIED
                                .name(),
                        request.getRemarks(),
                        request.getVerifiedBy()
                );

        when(verificationService.verifyProperty(
                APPLICATION_ID,
                request
        )).thenReturn(
                Mono.just(verificationRecord)
        );

        StepVerifier.create(
                        verificationController.verifyProperty(
                                APPLICATION_ID,
                                request
                        )
                )
                .assertNext(response -> {
                    assertEquals(
                            VERIFICATION_ID,
                            response.getVerificationId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            response.getApplicationId()
                    );

                    assertEquals(
                            VerificationStatus
                                    .PROPERTY_VERIFIED
                                    .name(),
                            response.getVerificationStatus()
                    );

                    assertEquals(
                            "Property documents verified",
                            response.getRemarks()
                    );

                    assertEquals(
                            "ADMIN",
                            response.getVerifiedBy()
                    );
                })
                .verifyComplete();

        verify(verificationService)
                .verifyProperty(
                        APPLICATION_ID,
                        request
                );
    }

    @Test
    void verifyProperty_ShouldPropagateServiceError() {

        PropertyVerificationRequest request =
                createPropertyVerificationRequest();

        RuntimeException exception =
                new RuntimeException(
                        "Verification failed"
                );

        when(verificationService.verifyProperty(
                APPLICATION_ID,
                request
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        verificationController.verifyProperty(
                                APPLICATION_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && error.getMessage().equals(
                                "Verification failed"
                        )
                )
                .verify();
    }

    // =========================================================
    // REQUEST INFORMATION TESTS
    // =========================================================

    @Test
    void requestInformation_ShouldReturnAdditionalInformationRecord() {

        AdditionalInfoRequest request =
                createAdditionalInfoRequest();

        VerificationRecord verificationRecord =
                createVerificationRecord(
                        VerificationStatus
                                .ADDITIONAL_INFORMATION_REQUIRED
                                .name(),
                        request.getRemarks(),
                        request.getRequestedBy()
                );

        when(verificationService.requestInformation(
                APPLICATION_ID,
                request
        )).thenReturn(
                Mono.just(verificationRecord)
        );

        StepVerifier.create(
                        verificationController.requestInformation(
                                APPLICATION_ID,
                                request
                        )
                )
                .assertNext(response -> {
                    assertEquals(
                            APPLICATION_ID,
                            response.getApplicationId()
                    );

                    assertEquals(
                            VerificationStatus
                                    .ADDITIONAL_INFORMATION_REQUIRED
                                    .name(),
                            response.getVerificationStatus()
                    );

                    assertEquals(
                            "Please upload the latest payment receipt",
                            response.getRemarks()
                    );

                    assertEquals(
                            "ADMIN",
                            response.getVerifiedBy()
                    );
                })
                .verifyComplete();

        verify(verificationService)
                .requestInformation(
                        APPLICATION_ID,
                        request
                );
    }

    @Test
    void requestInformation_ShouldPropagateServiceError() {

        AdditionalInfoRequest request =
                createAdditionalInfoRequest();

        RuntimeException exception =
                new RuntimeException(
                        "Unable to request information"
                );

        when(verificationService.requestInformation(
                APPLICATION_ID,
                request
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        verificationController.requestInformation(
                                APPLICATION_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                )
                .verify();
    }

    // =========================================================
    // APPROVAL TESTS
    // =========================================================

    @Test
    void approveVerification_ShouldReturnVerifiedRecord() {

        ApprovalRequest request =
                createApprovalRequest();

        VerificationRecord verificationRecord =
                createVerificationRecord(
                        VerificationStatus.VERIFIED.name(),
                        request.getRemarks(),
                        request.getApprovedBy()
                );

        when(verificationService.approveVerification(
                APPLICATION_ID,
                request
        )).thenReturn(
                Mono.just(verificationRecord)
        );

        StepVerifier.create(
                        verificationController
                                .approveVerification(
                                        APPLICATION_ID,
                                        request
                                )
                )
                .assertNext(response -> {
                    assertEquals(
                            APPLICATION_ID,
                            response.getApplicationId()
                    );

                    assertEquals(
                            VerificationStatus.VERIFIED.name(),
                            response.getVerificationStatus()
                    );

                    assertEquals(
                            "All verification checks completed successfully",
                            response.getRemarks()
                    );

                    assertEquals(
                            "ADMIN",
                            response.getVerifiedBy()
                    );
                })
                .verifyComplete();

        verify(verificationService)
                .approveVerification(
                        APPLICATION_ID,
                        request
                );
    }

    @Test
    void approveVerification_ShouldPropagateServiceError() {

        ApprovalRequest request =
                createApprovalRequest();

        RuntimeException exception =
                new RuntimeException(
                        "Risk Service unavailable"
                );

        when(verificationService.approveVerification(
                APPLICATION_ID,
                request
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        verificationController
                                .approveVerification(
                                        APPLICATION_ID,
                                        request
                                )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && error.getMessage().equals(
                                "Risk Service unavailable"
                        )
                )
                .verify();
    }

    // =========================================================
    // REJECTION TESTS
    // =========================================================

    @Test
    void rejectApplication_ShouldReturnRejectedRecord() {

        RejectVerificationRequest request =
                createRejectVerificationRequest();

        VerificationRecord verificationRecord =
                createVerificationRecord(
                        VerificationStatus.REJECTED.name(),
                        request.getRemarks(),
                        request.getRejectedBy()
                );

        when(verificationService.rejectApplication(
                APPLICATION_ID,
                request
        )).thenReturn(
                Mono.just(verificationRecord)
        );

        StepVerifier.create(
                        verificationController.rejectApplication(
                                APPLICATION_ID,
                                request
                        )
                )
                .assertNext(response -> {
                    assertEquals(
                            APPLICATION_ID,
                            response.getApplicationId()
                    );

                    assertEquals(
                            VerificationStatus.REJECTED.name(),
                            response.getVerificationStatus()
                    );

                    assertEquals(
                            "Property documents could not be validated",
                            response.getRemarks()
                    );

                    assertEquals(
                            "ADMIN",
                            response.getVerifiedBy()
                    );
                })
                .verifyComplete();

        verify(verificationService)
                .rejectApplication(
                        APPLICATION_ID,
                        request
                );
    }

    @Test
    void rejectApplication_ShouldPropagateServiceError() {

        RejectVerificationRequest request =
                createRejectVerificationRequest();

        RuntimeException exception =
                new RuntimeException(
                        "Unable to reject application"
                );

        when(verificationService.rejectApplication(
                APPLICATION_ID,
                request
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        verificationController.rejectApplication(
                                APPLICATION_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                )
                .verify();
    }

    // =========================================================
    // GET VERIFICATION RESULT TESTS
    // =========================================================

    @Test
    void getVerificationResult_ShouldReturnVerificationRecord() {

        VerificationRecord verificationRecord =
                createVerificationRecord(
                        VerificationStatus.VERIFIED.name(),
                        "Verification approved",
                        "ADMIN"
                );

        when(verificationService.getVerificationResult(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(verificationRecord)
        );

        StepVerifier.create(
                        verificationController
                                .getVerificationResult(
                                        APPLICATION_ID
                                )
                )
                .assertNext(response -> {
                    assertEquals(
                            VERIFICATION_ID,
                            response.getVerificationId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            response.getApplicationId()
                    );

                    assertEquals(
                            VerificationStatus.VERIFIED.name(),
                            response.getVerificationStatus()
                    );
                })
                .verifyComplete();

        verify(verificationService)
                .getVerificationResult(
                        APPLICATION_ID
                );
    }

    @Test
    void getVerificationResult_ShouldPropagateNotFoundError() {

        RuntimeException exception =
                new RuntimeException(
                        "Verification not found"
                );

        when(verificationService.getVerificationResult(
                APPLICATION_ID
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        verificationController
                                .getVerificationResult(
                                        APPLICATION_ID
                                )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && error.getMessage().equals(
                                "Verification not found"
                        )
                )
                .verify();
    }

    // =========================================================
    // TEST DATA METHODS
    // =========================================================

    private PropertyVerificationRequest
    createPropertyVerificationRequest() {

        PropertyVerificationRequest request =
                new PropertyVerificationRequest();

        request.setRemarks(
                "Property documents verified"
        );

        request.setVerifiedBy(
                "ADMIN"
        );

        return request;
    }

    private AdditionalInfoRequest
    createAdditionalInfoRequest() {

        AdditionalInfoRequest request =
                new AdditionalInfoRequest();

        request.setRemarks(
                "Please upload the latest payment receipt"
        );

        request.setRequestedBy(
                "ADMIN"
        );

        return request;
    }

    private ApprovalRequest createApprovalRequest() {

        ApprovalRequest request =
                new ApprovalRequest();

        request.setRemarks(
                "All verification checks completed successfully"
        );

        request.setApprovedBy(
                "ADMIN"
        );

        return request;
    }

    private RejectVerificationRequest
    createRejectVerificationRequest() {

        RejectVerificationRequest request =
                new RejectVerificationRequest();

        request.setRemarks(
                "Property documents could not be validated"
        );

        request.setRejectedBy(
                "ADMIN"
        );

        return request;
    }

    private VerificationRecord createVerificationRecord(
            String status,
            String remarks,
            String verifiedBy
    ) {
        VerificationRecord verificationRecord =
                new VerificationRecord();

        verificationRecord.setVerificationId(
                VERIFICATION_ID
        );

        verificationRecord.setApplicationId(
                APPLICATION_ID
        );

        verificationRecord.setVerificationStatus(
                status
        );

        verificationRecord.setRemarks(
                remarks
        );

        verificationRecord.setVerifiedBy(
                verifiedBy
        );

        verificationRecord.setVerifiedAt(
                LocalDateTime.of(
                        2026,
                        10,
                        4,
                        18,
                        30
                )
        );

        verificationRecord.setNew(false);

        return verificationRecord;
    }

    private ApplicationResponse createApplicationResponse(
            String applicationId,
            Integer userId,
            String status
    ) {
        ApplicationResponse application =
                new ApplicationResponse();

        application.setApplicationId(
                applicationId
        );

        application.setUserId(
                userId
        );

        application.setBuilderName(
                "Apexes Infrastructure"
        );

        application.setProjectName(
                "Skyline Heights"
        );

        application.setUnitNumber(
                "B-1404"
        );

        application.setPropertyValue(
                new BigDecimal("12500000.00")
        );

        application.setPurchaseDate(
                LocalDate.of(2024, 11, 10)
        );

        application.setPromisedPossessionDate(
                LocalDate.of(2026, 12, 31)
        );

        application.setStatus(status);

        return application;
    }
}