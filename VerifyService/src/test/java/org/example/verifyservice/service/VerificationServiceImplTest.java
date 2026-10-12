package org.example.verifyservice.service;

import org.example.verifyservice.dto.AdditionalInfoRequest;
import org.example.verifyservice.dto.ApprovalRequest;
import org.example.verifyservice.dto.PropertyVerificationRequest;
import org.example.verifyservice.dto.RejectVerificationRequest;
import org.example.verifyservice.entity.VerificationRecord;
import org.example.verifyservice.enums.VerificationStatus;
import org.example.verifyservice.exception.InvalidVerificationRequestException;
import org.example.verifyservice.exception.VerificationAlreadyCompletedException;
import org.example.verifyservice.exception.VerificationNotFoundException;
import org.example.verifyservice.repository.VerificationRepository;
import org.example.verifyservice.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveInsertOperation;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationServiceImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    private VerificationRepository verificationRepository;

    @Mock
    private R2dbcEntityTemplate template;

    @Mock
    private ReactiveInsertOperation.ReactiveInsert<VerificationRecord>
            verificationInsert;

    @Mock
    private JwtService jwtService;

    private VerificationServiceImpl verificationService;

    private static final String APPLICATION_ID =
            "application-101";

    private static final String VERIFICATION_ID =
            "verification-101";

    @BeforeEach
    void setUp() {

        StepVerifier.setDefaultTimeout(
                Duration.ofSeconds(5)
        );

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
    // PROPERTY VERIFICATION INSERT TEST
    // =========================================================

    @Test
    void verifyProperty_ShouldInsertNewRecord_WhenRecordDoesNotExist() {

        PropertyVerificationRequest request =
                createPropertyVerificationRequest();

        when(verificationRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.empty()
        );

        when(template.insert(VerificationRecord.class))
                .thenReturn(verificationInsert);

        when(verificationInsert.using(
                any(VerificationRecord.class)
        )).thenAnswer(invocation -> {
            VerificationRecord record =
                    invocation.getArgument(0);

            return Mono.just(record);
        });

        StepVerifier.create(
                        verificationService.verifyProperty(
                                APPLICATION_ID,
                                request
                        )
                )
                .assertNext(record -> {
                    assertNotNull(
                            record.getVerificationId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            record.getApplicationId()
                    );

                    assertEquals(
                            VerificationStatus.PROPERTY_VERIFIED.name(),
                            record.getVerificationStatus()
                    );

                    assertEquals(
                            request.getRemarks(),
                            record.getRemarks()
                    );

                    assertEquals(
                            request.getVerifiedBy(),
                            record.getVerifiedBy()
                    );

                    assertNotNull(
                            record.getVerifiedAt()
                    );

                    assertTrue(
                            record.isNew()
                    );
                })
                .verifyComplete();

        verify(verificationRepository)
                .findByApplicationId(APPLICATION_ID);

        verify(verificationInsert)
                .using(any(VerificationRecord.class));
    }

    // =========================================================
    // PROPERTY VERIFICATION UPDATE TEST
    // =========================================================

    @Test
    void verifyProperty_ShouldUpdateExistingRecord() {

        PropertyVerificationRequest request =
                createPropertyVerificationRequest();

        VerificationRecord existingRecord =
                createVerificationRecord(
                        VerificationStatus.PENDING.name()
                );

        when(verificationRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(existingRecord)
        );

        when(template.update(existingRecord))
                .thenAnswer(invocation -> {
                    VerificationRecord record =
                            invocation.getArgument(0);

                    return Mono.just(record);
                });

        StepVerifier.create(
                        verificationService.verifyProperty(
                                APPLICATION_ID,
                                request
                        )
                )
                .assertNext(record -> {
                    assertEquals(
                            VERIFICATION_ID,
                            record.getVerificationId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            record.getApplicationId()
                    );

                    assertEquals(
                            VerificationStatus.PROPERTY_VERIFIED.name(),
                            record.getVerificationStatus()
                    );

                    assertEquals(
                            request.getRemarks(),
                            record.getRemarks()
                    );

                    assertEquals(
                            request.getVerifiedBy(),
                            record.getVerifiedBy()
                    );

                    assertNotNull(
                            record.getVerifiedAt()
                    );

                    assertFalse(
                            record.isNew()
                    );
                })
                .verifyComplete();

        verify(verificationRepository)
                .findByApplicationId(APPLICATION_ID);

        verify(template)
                .update(existingRecord);
    }

    // =========================================================
    // REQUEST INFORMATION INSERT TEST
    // =========================================================

    @Test
    void requestInformation_ShouldInsertNewRecord_WhenRecordDoesNotExist() {

        AdditionalInfoRequest request =
                createAdditionalInfoRequest();

        when(verificationRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.empty()
        );

        when(template.insert(VerificationRecord.class))
                .thenReturn(verificationInsert);

        when(verificationInsert.using(
                any(VerificationRecord.class)
        )).thenAnswer(invocation -> {
            VerificationRecord record =
                    invocation.getArgument(0);

            return Mono.just(record);
        });

        StepVerifier.create(
                        verificationService.requestInformation(
                                APPLICATION_ID,
                                request
                        )
                )
                .assertNext(record -> {
                    assertNotNull(
                            record.getVerificationId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            record.getApplicationId()
                    );

                    assertEquals(
                            VerificationStatus
                                    .ADDITIONAL_INFORMATION_REQUIRED
                                    .name(),
                            record.getVerificationStatus()
                    );

                    assertEquals(
                            request.getRemarks(),
                            record.getRemarks()
                    );

                    assertEquals(
                            request.getRequestedBy(),
                            record.getVerifiedBy()
                    );

                    assertNotNull(
                            record.getVerifiedAt()
                    );

                    assertTrue(
                            record.isNew()
                    );
                })
                .verifyComplete();

        verify(verificationInsert)
                .using(any(VerificationRecord.class));
    }

    // =========================================================
    // REQUEST INFORMATION UPDATE TEST
    // =========================================================

    @Test
    void requestInformation_ShouldUpdateExistingPropertyVerifiedRecord() {

        AdditionalInfoRequest request =
                createAdditionalInfoRequest();

        VerificationRecord existingRecord =
                createVerificationRecord(
                        VerificationStatus.PROPERTY_VERIFIED.name()
                );

        when(verificationRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(existingRecord)
        );

        when(template.update(existingRecord))
                .thenAnswer(invocation -> {
                    VerificationRecord record =
                            invocation.getArgument(0);

                    return Mono.just(record);
                });

        StepVerifier.create(
                        verificationService.requestInformation(
                                APPLICATION_ID,
                                request
                        )
                )
                .assertNext(record -> {
                    assertEquals(
                            VERIFICATION_ID,
                            record.getVerificationId()
                    );

                    assertEquals(
                            VerificationStatus
                                    .ADDITIONAL_INFORMATION_REQUIRED
                                    .name(),
                            record.getVerificationStatus()
                    );

                    assertEquals(
                            request.getRemarks(),
                            record.getRemarks()
                    );

                    assertEquals(
                            request.getRequestedBy(),
                            record.getVerifiedBy()
                    );

                    assertFalse(
                            record.isNew()
                    );
                })
                .verifyComplete();

        verify(template)
                .update(existingRecord);
    }

    // =========================================================
    // FINAL-STATE PROTECTION TESTS
    // =========================================================

    @Test
    void verifyProperty_ShouldThrowException_WhenAlreadyVerified() {

        PropertyVerificationRequest request =
                createPropertyVerificationRequest();

        VerificationRecord existingRecord =
                createVerificationRecord(
                        VerificationStatus.VERIFIED.name()
                );

        when(verificationRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(existingRecord)
        );

        StepVerifier.create(
                        verificationService.verifyProperty(
                                APPLICATION_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                VerificationAlreadyCompletedException
                                && error.getMessage().equals(
                                "Verification is already completed as VERIFIED"
                        )
                )
                .verify();

        verify(template, never())
                .update(any(VerificationRecord.class));
    }

    @Test
    void verifyProperty_ShouldThrowException_WhenAlreadyRejected() {

        PropertyVerificationRequest request =
                createPropertyVerificationRequest();

        VerificationRecord existingRecord =
                createVerificationRecord(
                        VerificationStatus.REJECTED.name()
                );

        when(verificationRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(existingRecord)
        );

        StepVerifier.create(
                        verificationService.verifyProperty(
                                APPLICATION_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                VerificationAlreadyCompletedException
                                && error.getMessage().equals(
                                "Verification is already completed as REJECTED"
                        )
                )
                .verify();

        verify(template, never())
                .update(any(VerificationRecord.class));
    }

    @Test
    void requestInformation_ShouldThrowException_WhenAlreadyVerified() {

        AdditionalInfoRequest request =
                createAdditionalInfoRequest();

        VerificationRecord existingRecord =
                createVerificationRecord(
                        VerificationStatus.VERIFIED.name()
                );

        when(verificationRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(existingRecord)
        );

        StepVerifier.create(
                        verificationService.requestInformation(
                                APPLICATION_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                VerificationAlreadyCompletedException
                                && error.getMessage().equals(
                                "Verification is already completed as VERIFIED"
                        )
                )
                .verify();

        verify(template, never())
                .update(any(VerificationRecord.class));
    }

    @Test
    void requestInformation_ShouldThrowException_WhenAlreadyRejected() {

        AdditionalInfoRequest request =
                createAdditionalInfoRequest();

        VerificationRecord existingRecord =
                createVerificationRecord(
                        VerificationStatus.REJECTED.name()
                );

        when(verificationRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(existingRecord)
        );

        StepVerifier.create(
                        verificationService.requestInformation(
                                APPLICATION_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                VerificationAlreadyCompletedException
                                && error.getMessage().equals(
                                "Verification is already completed as REJECTED"
                        )
                )
                .verify();

        verify(template, never())
                .update(any(VerificationRecord.class));
    }

    // =========================================================
    // PROPERTY VERIFICATION VALIDATION TESTS
    // =========================================================

    @Test
    void verifyProperty_ShouldThrowException_WhenApplicationIdIsNull() {

        PropertyVerificationRequest request =
                createPropertyVerificationRequest();

        StepVerifier.create(
                        Mono.defer(() ->
                                verificationService.verifyProperty(
                                        null,
                                        request
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidVerificationRequestException
                                && error.getMessage().equals(
                                "Application ID is required"
                        )
                )
                .verify();

        verify(verificationRepository, never())
                .findByApplicationId(any());
    }

    @Test
    void verifyProperty_ShouldThrowException_WhenApplicationIdIsBlank() {

        PropertyVerificationRequest request =
                createPropertyVerificationRequest();

        StepVerifier.create(
                        Mono.defer(() ->
                                verificationService.verifyProperty(
                                        "   ",
                                        request
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidVerificationRequestException
                                && error.getMessage().equals(
                                "Application ID is required"
                        )
                )
                .verify();

        verify(verificationRepository, never())
                .findByApplicationId(any());
    }

    @Test
    void verifyProperty_ShouldThrowException_WhenRequestIsNull() {

        StepVerifier.create(
                        verificationService.verifyProperty(
                                APPLICATION_ID,
                                null
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidVerificationRequestException
                                && error.getMessage().equals(
                                "Property verification request is required"
                        )
                )
                .verify();

        verify(verificationRepository, never())
                .findByApplicationId(any());
    }

    @Test
    void verifyProperty_ShouldThrowException_WhenRemarksAreBlank() {

        PropertyVerificationRequest request =
                createPropertyVerificationRequest();

        request.setRemarks("   ");

        StepVerifier.create(
                        Mono.defer(() ->
                                verificationService.verifyProperty(
                                        APPLICATION_ID,
                                        request
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidVerificationRequestException
                                && error.getMessage().equals(
                                "Remarks are required"
                        )
                )
                .verify();

        verify(verificationRepository, never())
                .findByApplicationId(any());
    }

    @Test
    void verifyProperty_ShouldThrowException_WhenVerifiedByIsBlank() {

        PropertyVerificationRequest request =
                createPropertyVerificationRequest();

        request.setVerifiedBy("");

        StepVerifier.create(
                        Mono.defer(() ->
                                verificationService.verifyProperty(
                                        APPLICATION_ID,
                                        request
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidVerificationRequestException
                                && error.getMessage().equals(
                                "Verified by is required"
                        )
                )
                .verify();

        verify(verificationRepository, never())
                .findByApplicationId(any());
    }

    // =========================================================
    // REQUEST INFORMATION VALIDATION TESTS
    // =========================================================

    @Test
    void requestInformation_ShouldThrowException_WhenRequestIsNull() {

        StepVerifier.create(
                        verificationService.requestInformation(
                                APPLICATION_ID,
                                null
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidVerificationRequestException
                                && error.getMessage().equals(
                                "Additional information request is required"
                        )
                )
                .verify();

        verify(verificationRepository, never())
                .findByApplicationId(any());
    }

    @Test
    void requestInformation_ShouldThrowException_WhenRemarksAreMissing() {

        AdditionalInfoRequest request =
                createAdditionalInfoRequest();

        request.setRemarks(null);

        StepVerifier.create(
                        Mono.defer(() ->
                                verificationService.requestInformation(
                                        APPLICATION_ID,
                                        request
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidVerificationRequestException
                                && error.getMessage().equals(
                                "Remarks are required"
                        )
                )
                .verify();

        verify(verificationRepository, never())
                .findByApplicationId(any());
    }

    @Test
    void requestInformation_ShouldThrowException_WhenRequestedByIsMissing() {

        AdditionalInfoRequest request =
                createAdditionalInfoRequest();

        request.setRequestedBy(null);

        StepVerifier.create(
                        Mono.defer(() ->
                                verificationService.requestInformation(
                                        APPLICATION_ID,
                                        request
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidVerificationRequestException
                                && error.getMessage().equals(
                                "Requested by is required"
                        )
                )
                .verify();

        verify(verificationRepository, never())
                .findByApplicationId(any());
    }

    // =========================================================
    // APPROVAL VALIDATION TESTS
    // =========================================================

    @Test
    void approveVerification_ShouldThrowException_WhenRequestIsNull() {

        StepVerifier.create(
                        verificationService.approveVerification(
                                APPLICATION_ID,
                                null
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidVerificationRequestException
                                && error.getMessage().equals(
                                "Approval request is required"
                        )
                )
                .verify();

        verify(verificationRepository, never())
                .findByApplicationId(any());
    }

    @Test
    void approveVerification_ShouldThrowException_WhenRemarksAreBlank() {

        ApprovalRequest request =
                createApprovalRequest();

        request.setRemarks("");

        StepVerifier.create(
                        Mono.defer(() ->
                                verificationService.approveVerification(
                                        APPLICATION_ID,
                                        request
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidVerificationRequestException
                                && error.getMessage().equals(
                                "Remarks are required"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();

        verify(verificationRepository, never())
                .findByApplicationId(any());
    }

    @Test
    void approveVerification_ShouldThrowException_WhenApprovedByIsBlank() {

        ApprovalRequest request =
                createApprovalRequest();

        request.setApprovedBy("   ");

        StepVerifier.create(
                        Mono.defer(() ->
                                verificationService.approveVerification(
                                        APPLICATION_ID,
                                        request
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidVerificationRequestException
                                && error.getMessage().equals(
                                "Approved by is required"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();

        verify(verificationRepository, never())
                .findByApplicationId(any());
    }

    // =========================================================
    // REJECTION VALIDATION TESTS
    // =========================================================

    @Test
    void rejectApplication_ShouldThrowException_WhenRequestIsNull() {

        StepVerifier.create(
                        verificationService.rejectApplication(
                                APPLICATION_ID,
                                null
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidVerificationRequestException
                                && error.getMessage().equals(
                                "Rejection request is required"
                        )
                )
                .verify();

        verify(verificationRepository, never())
                .findByApplicationId(any());
    }

    @Test
    void rejectApplication_ShouldThrowException_WhenRemarksAreBlank() {

        RejectVerificationRequest request =
                createRejectRequest();

        request.setRemarks(" ");

        StepVerifier.create(
                        Mono.defer(() ->
                                verificationService.rejectApplication(
                                        APPLICATION_ID,
                                        request
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                InvalidVerificationRequestException
                                && error.getMessage().equals(
                                "Remarks are required"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();

        verify(verificationRepository, never())
                .findByApplicationId(any());
    }

    @Test
    void rejectApplication_ShouldThrowException_WhenRejectedByIsBlank() {

        RejectVerificationRequest request =
                createRejectRequest();

        request.setRejectedBy(                ""
        );

        StepVerifier.create(
                        Mono.defer(() ->
                                verificationService.rejectApplication(
                                        APPLICATION_ID,
                                        request
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidVerificationRequestException
                                && error.getMessage().equals(
                                "Rejected by is required"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();

        verify(verificationRepository, never())
                .findByApplicationId(any());
    }

    // =========================================================
    // GET VERIFICATION RESULT TESTS
    // =========================================================

    @Test
    void getVerificationResult_ShouldReturnRecord_WhenRecordExists() {

        VerificationRecord record =
                createVerificationRecord(
                        VerificationStatus.VERIFIED.name()
                );

        when(verificationRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(record)
        );

        StepVerifier.create(
                        verificationService.getVerificationResult(
                                APPLICATION_ID
                        )
                )
                .expectNext(record)
                .verifyComplete();

        verify(verificationRepository)
                .findByApplicationId(APPLICATION_ID);
    }

    @Test
    void getVerificationResult_ShouldThrowNotFound_WhenRecordDoesNotExist() {

        when(verificationRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        verificationService.getVerificationResult(
                                APPLICATION_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof VerificationNotFoundException
                                && error.getMessage().equals(
                                "No verification record found for application: "
                                        + APPLICATION_ID
                        )
                )
                .verify();
    }

    @Test
    void getVerificationResult_ShouldThrowException_WhenApplicationIdIsBlank() {

        StepVerifier.create(
                        Mono.defer(() ->
                                verificationService
                                        .getVerificationResult(" ")
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidVerificationRequestException
                                && error.getMessage().equals(
                                "Application ID is required"
                        )
                )
                .verify();

        verify(verificationRepository, never())
                .findByApplicationId(any());
    }

    // =========================================================
    // TEST DATA HELPERS
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
                "Verification approved"
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