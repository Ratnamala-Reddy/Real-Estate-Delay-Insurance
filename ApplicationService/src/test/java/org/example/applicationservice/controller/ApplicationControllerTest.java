package org.example.applicationservice.controller;

import org.example.applicationservice.Service.ApplicationServiceImpl;
import org.example.applicationservice.dto.ApplicationStatusUpdateRequest;
import org.example.applicationservice.dto.CreateApplicationRequest;
import org.example.applicationservice.dto.UploadDocumentRequest;
import org.example.applicationservice.Entity.Application;
import org.example.applicationservice.Entity.ApplicationDocument;
import org.example.applicationservice.enums.ApplicationStatus;
import org.example.applicationservice.exceptions.ApplicationNotFoundException;
import org.example.applicationservice.security.JwtService;
import org.example.applicationservice.Service.ApplicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock
    private ApplicationServiceImpl applicationService;

    @Mock
    private JwtService jwtService;

    private ApplicationController applicationController;

    private static final Integer USER_ID = 4;

    private static final Integer OTHER_USER_ID = 5;

    private static final String APPLICATION_ID =
            "application-101";

    private static final String BUYER_TOKEN =
            "valid-buyer-token";

    private static final String ADMIN_TOKEN =
            "valid-admin-token";

    @BeforeEach
    void setUp() {
        applicationController =
                new ApplicationController(
                        applicationService,
                        jwtService
                );
    }

    // =========================================================
    // CREATE APPLICATION TESTS
    // =========================================================

    @Test
    void createApplication_ShouldCreateApplication_WhenBuyerTokenIsValid() {

        CreateApplicationRequest request =
                createApplicationRequest();

        Application application =
                createApplication(
                        USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        mockBuyerClaims(BUYER_TOKEN, USER_ID);

        when(applicationService.createApplication(
                USER_ID,
                request
        )).thenReturn(
                Mono.just(application)
        );

        StepVerifier.create(
                        applicationController.createApplication(
                                null,
                                null,
                                null,
                                BUYER_TOKEN,
                                null,
                                request
                        )
                )
                .expectNext(application)
                .verifyComplete();

        verify(jwtService)
                .extractUserId(BUYER_TOKEN);

        verify(jwtService)
                .extractRole(BUYER_TOKEN);

        verify(jwtService)
                .extractEmail(BUYER_TOKEN);

        verify(applicationService)
                .createApplication(
                        USER_ID,
                        request
                );
    }

    @Test
    void createApplication_ShouldUseJwtIdentity_InsteadOfHeaders() {

        CreateApplicationRequest request =
                createApplicationRequest();

        Application application =
                createApplication(
                        USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        mockBuyerClaims(BUYER_TOKEN, USER_ID);

        when(applicationService.createApplication(
                USER_ID,
                request
        )).thenReturn(
                Mono.just(application)
        );

        StepVerifier.create(
                        applicationController.createApplication(
                                "999",
                                "ADMIN",
                                "fake@gmail.com",
                                BUYER_TOKEN,
                                null,
                                request
                        )
                )
                .assertNext(response ->
                        assertEquals(
                                USER_ID,
                                response.getUserId()
                        )
                )
                .verifyComplete();

        verify(applicationService)
                .createApplication(
                        USER_ID,
                        request
                );

        verify(applicationService, never())
                .createApplication(
                        999,
                        request
                );
    }

    @Test
    void createApplication_ShouldUseBearerToken() {

        CreateApplicationRequest request =
                createApplicationRequest();

        Application application =
                createApplication(
                        USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        mockBuyerClaims(BUYER_TOKEN, USER_ID);

        when(applicationService.createApplication(
                USER_ID,
                request
        )).thenReturn(
                Mono.just(application)
        );

        StepVerifier.create(
                        applicationController.createApplication(
                                null,
                                null,
                                null,
                                null,
                                "Bearer " + BUYER_TOKEN,
                                request
                        )
                )
                .expectNext(application)
                .verifyComplete();

        verify(jwtService)
                .extractUserId(BUYER_TOKEN);

        verify(applicationService)
                .createApplication(
                        USER_ID,
                        request
                );
    }

    @Test
    void createApplication_ShouldThrowException_WhenUserIdIsMissing() {

        CreateApplicationRequest request =
                createApplicationRequest();

        StepVerifier.create(
                        applicationController.createApplication(
                                null,
                                "BUYER",
                                "buyer@gmail.com",
                                null,
                                null,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof IllegalArgumentException
                                && error.getMessage().equals(
                                "Authenticated user ID is required"
                        )
                )
                .verify();

        verify(applicationService, never())
                .createApplication(
                        any(),
                        any(CreateApplicationRequest.class)
                );
    }

    @Test
    void createApplication_ShouldThrowException_WhenHeaderUserIdIsInvalid() {

        CreateApplicationRequest request =
                createApplicationRequest();

        StepVerifier.create(
                        Mono.defer(() ->
                                applicationController
                                        .createApplication(
                                                "invalid-user-id",
                                                "BUYER",
                                                "buyer@gmail.com",
                                                null,
                                                null,
                                                request
                                        )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof IllegalArgumentException
                                && error.getMessage().equals(
                                "Invalid X-User-Id header"
                        )
                )
                .verify();

        verify(applicationService, never())
                .createApplication(
                        any(),
                        any(CreateApplicationRequest.class)
                );
    }

    // =========================================================
    // GET APPLICATION TESTS
    // =========================================================

    @Test
    void getApplication_ShouldReturnApplication_WhenBuyerOwnsApplication() {

        Application application =
                createApplication(
                        USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        mockBuyerClaims(BUYER_TOKEN, USER_ID);

        when(applicationService.getApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(application)
        );

        StepVerifier.create(
                        applicationController.getApplication(
                                APPLICATION_ID,
                                null,
                                null,
                                null,
                                BUYER_TOKEN,
                                null
                        )
                )
                .expectNext(application)
                .verifyComplete();

        verify(applicationService)
                .getApplication(APPLICATION_ID);
    }

    @Test
    void getApplication_ShouldThrowAccessDenied_WhenBuyerDoesNotOwnApplication() {

        Application application =
                createApplication(
                        OTHER_USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        mockBuyerClaims(BUYER_TOKEN, USER_ID);

        when(applicationService.getApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(application)
        );

        StepVerifier.create(
                        applicationController.getApplication(
                                APPLICATION_ID,
                                null,
                                null,
                                null,
                                BUYER_TOKEN,
                                null
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof AccessDeniedException
                                && error.getMessage().equals(
                                "Access Denied: You do not own this application"
                        )
                )
                .verify();
    }

    @Test
    void getApplication_ShouldAllowAdmin() {

        Application application =
                createApplication(
                        OTHER_USER_ID,
                        ApplicationStatus.SUBMITTED.name()
                );

        mockAdminClaims(ADMIN_TOKEN);

        when(applicationService.getApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(application)
        );

        StepVerifier.create(
                        applicationController.getApplication(
                                APPLICATION_ID,
                                null,
                                null,
                                null,
                                ADMIN_TOKEN,
                                null
                        )
                )
                .expectNext(application)
                .verifyComplete();
    }

    @Test
    void getApplication_ShouldThrowNotFound_WhenApplicationDoesNotExist() {

        mockBuyerClaims(BUYER_TOKEN, USER_ID);

        when(applicationService.getApplication(
                APPLICATION_ID
        )).thenReturn(Mono.empty());

        StepVerifier.create(
                        applicationController.getApplication(
                                APPLICATION_ID,
                                null,
                                null,
                                null,
                                BUYER_TOKEN,
                                null
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ApplicationNotFoundException
                                && error.getMessage().equals(
                                "Application not found with id: "
                                        + APPLICATION_ID
                        )
                )
                .verify();
    }

    // =========================================================
    // GET MY APPLICATIONS TESTS
    // =========================================================

    @Test
    void getMyApplications_ShouldReturnBuyerApplications() {

        Application first =
                createApplication(
                        USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        Application second =
                createApplication(
                        USER_ID,
                        ApplicationStatus.SUBMITTED.name()
                );

        second.setApplicationId("application-102");

        mockBuyerClaims(BUYER_TOKEN, USER_ID);

        when(applicationService.getMyApplications(USER_ID))
                .thenReturn(
                        Flux.just(first, second)
                );

        StepVerifier.create(
                        applicationController.getMyApplications(
                                null,
                                null,
                                null,
                                BUYER_TOKEN,
                                null
                        )
                )
                .expectNext(first)
                .expectNext(second)
                .verifyComplete();

        verify(applicationService)
                .getMyApplications(USER_ID);
    }

    @Test
    void getMyApplications_ShouldThrowAccessDenied_WhenUserIdIsMissing() {

        StepVerifier.create(
                        applicationController.getMyApplications(
                                null,
                                "BUYER",
                                "buyer@gmail.com",
                                null,
                                null
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof AccessDeniedException
                                && error.getMessage().equals(
                                "Access Denied: User not authenticated"
                        )
                )
                .verify();

        verify(applicationService, never())
                .getMyApplications(any());
    }

    // =========================================================
    // GET ALL APPLICATIONS TEST
    // =========================================================

    @Test
    void getAllApplications_ShouldReturnAllApplications() {

        Application first =
                createApplication(
                        USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        Application second =
                createApplication(
                        OTHER_USER_ID,
                        ApplicationStatus.SUBMITTED.name()
                );

        second.setApplicationId("application-102");

        when(applicationService.getAllApplications())
                .thenReturn(
                        Flux.just(first, second)
                );

        StepVerifier.create(
                        applicationController.getAllApplications()
                )
                .expectNext(first)
                .expectNext(second)
                .verifyComplete();

        verify(applicationService)
                .getAllApplications();
    }

    // =========================================================
    // UPDATE STATUS TEST
    // =========================================================

    @Test
    void updateApplicationStatus_ShouldDelegateToService() {

        ApplicationStatusUpdateRequest request =
                new ApplicationStatusUpdateRequest();

        request.setStatus(
                ApplicationStatus
                        .VERIFICATION_IN_PROGRESS
                        .name()
        );

        Application application =
                createApplication(
                        USER_ID,
                        ApplicationStatus
                                .VERIFICATION_IN_PROGRESS
                                .name()
                );

        when(applicationService.updateApplicationStatus(
                APPLICATION_ID,
                request.getStatus()
        )).thenReturn(
                Mono.just(application)
        );

        StepVerifier.create(
                        applicationController
                                .updateApplicationStatus(
                                        APPLICATION_ID,
                                        request
                                )
                )
                .expectNext(application)
                .verifyComplete();

        verify(applicationService)
                .updateApplicationStatus(
                        APPLICATION_ID,
                        request.getStatus()
                );
    }

    // =========================================================
    // UPDATE APPLICATION TESTS
    // =========================================================

    @Test
    void updateApplication_ShouldUpdate_WhenBuyerOwnsApplication() {

        Application application =
                createApplication(
                        USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        Application updatedApplication =
                createApplication(
                        USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        updatedApplication.setBuilderName(
                "Updated Builders"
        );

        CreateApplicationRequest request =
                createApplicationRequest();

        mockBuyerClaims(BUYER_TOKEN, USER_ID);

        when(applicationService.getApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(application)
        );

        when(applicationService.updateApplication(
                APPLICATION_ID,
                request
        )).thenReturn(
                Mono.just(updatedApplication)
        );

        StepVerifier.create(
                        applicationController.updateApplication(
                                APPLICATION_ID,
                                null,
                                null,
                                null,
                                BUYER_TOKEN,
                                null,
                                request
                        )
                )
                .assertNext(response ->
                        assertEquals(
                                "Updated Builders",
                                response.getBuilderName()
                        )
                )
                .verifyComplete();

        verify(applicationService)
                .updateApplication(
                        APPLICATION_ID,
                        request
                );
    }

    @Test
    void updateApplication_ShouldThrowAccessDenied_WhenBuyerDoesNotOwnApplication() {

        Application application =
                createApplication(
                        OTHER_USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        CreateApplicationRequest request =
                createApplicationRequest();

        mockBuyerClaims(BUYER_TOKEN, USER_ID);

        when(applicationService.getApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(application)
        );

        StepVerifier.create(
                        applicationController.updateApplication(
                                APPLICATION_ID,
                                null,
                                null,
                                null,
                                BUYER_TOKEN,
                                null,
                                request
                        )
                )
                .expectError(AccessDeniedException.class)
                .verify();

        verify(applicationService, never())
                .updateApplication(
                        APPLICATION_ID,
                        request
                );
    }

    // =========================================================
    // GET DOCUMENTS TESTS
    // =========================================================

    @Test
    void getDocuments_ShouldReturnDocuments_WhenBuyerOwnsApplication() {

        Application application =
                createApplication(
                        USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        ApplicationDocument document =
                createDocument();

        mockBuyerClaims(BUYER_TOKEN, USER_ID);

        when(applicationService.getApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(application)
        );

        when(applicationService.getDocuments(
                APPLICATION_ID
        )).thenReturn(
                Flux.just(document)
        );

        StepVerifier.create(
                        applicationController.getDocuments(
                                APPLICATION_ID,
                                null,
                                null,
                                null,
                                BUYER_TOKEN,
                                null
                        )
                )
                .expectNext(document)
                .verifyComplete();

        verify(applicationService)
                .getDocuments(APPLICATION_ID);
    }

    @Test
    void getDocuments_ShouldAllowAdmin() {

        Application application =
                createApplication(
                        OTHER_USER_ID,
                        ApplicationStatus.SUBMITTED.name()
                );

        ApplicationDocument document =
                createDocument();

        mockAdminClaims(ADMIN_TOKEN);

        when(applicationService.getApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(application)
        );

        when(applicationService.getDocuments(
                APPLICATION_ID
        )).thenReturn(
                Flux.just(document)
        );

        StepVerifier.create(
                        applicationController.getDocuments(
                                APPLICATION_ID,
                                null,
                                null,
                                null,
                                ADMIN_TOKEN,
                                null
                        )
                )
                .expectNext(document)
                .verifyComplete();

        verify(applicationService)
                .getDocuments(APPLICATION_ID);
    }

    @Test
    void getDocuments_ShouldThrowAccessDenied_WhenBuyerDoesNotOwnApplication() {

        Application application =
                createApplication(
                        OTHER_USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        mockBuyerClaims(BUYER_TOKEN, USER_ID);

        when(applicationService.getApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(application)
        );

        StepVerifier.create(
                        applicationController.getDocuments(
                                APPLICATION_ID,
                                null,
                                null,
                                null,
                                BUYER_TOKEN,
                                null
                        )
                )
                .expectError(AccessDeniedException.class)
                .verify();

        verify(applicationService, never())
                .getDocuments(APPLICATION_ID);
    }

    // =========================================================
    // UPLOAD DOCUMENT TESTS
    // =========================================================

    @Test
    void uploadDocument_ShouldUpload_WhenBuyerOwnsApplication() {

        Application application =
                createApplication(
                        USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        UploadDocumentRequest request =
                createUploadDocumentRequest();

        ApplicationDocument document =
                createDocument();

        mockBuyerClaims(BUYER_TOKEN, USER_ID);

        when(applicationService.getApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(application)
        );

        when(applicationService.uploadDocument(
                APPLICATION_ID,
                request
        )).thenReturn(
                Mono.just(document)
        );

        StepVerifier.create(
                        applicationController.uploadDocument(
                                APPLICATION_ID,
                                null,
                                null,
                                null,
                                BUYER_TOKEN,
                                null,
                                request
                        )
                )
                .expectNext(document)
                .verifyComplete();

        verify(applicationService)
                .uploadDocument(
                        APPLICATION_ID,
                        request
                );
    }

    @Test
    void uploadDocument_ShouldThrowAccessDenied_WhenBuyerDoesNotOwnApplication() {

        Application application =
                createApplication(
                        OTHER_USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        UploadDocumentRequest request =
                createUploadDocumentRequest();

        mockBuyerClaims(BUYER_TOKEN, USER_ID);

        when(applicationService.getApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(application)
        );

        StepVerifier.create(
                        applicationController.uploadDocument(
                                APPLICATION_ID,
                                null,
                                null,
                                null,
                                BUYER_TOKEN,
                                null,
                                request
                        )
                )
                .expectError(AccessDeniedException.class)
                .verify();

        verify(applicationService, never())
                .uploadDocument(
                        APPLICATION_ID,
                        request
                );
    }

    // =========================================================
    // SUBMIT APPLICATION TESTS
    // =========================================================

    @Test
    void submitApplication_ShouldSubmit_WhenBuyerOwnsApplication() {

        Application application =
                createApplication(
                        USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        Application submittedApplication =
                createApplication(
                        USER_ID,
                        ApplicationStatus.SUBMITTED.name()
                );

        mockBuyerClaims(BUYER_TOKEN, USER_ID);

        when(applicationService.getApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(application)
        );

        when(applicationService.submitApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(submittedApplication)
        );

        StepVerifier.create(
                        applicationController.submitApplication(
                                APPLICATION_ID,
                                null,
                                null,
                                null,
                                BUYER_TOKEN,
                                null
                        )
                )
                .assertNext(response ->
                        assertEquals(
                                ApplicationStatus.SUBMITTED.name(),
                                response.getStatus()
                        )
                )
                .verifyComplete();

        verify(applicationService)
                .submitApplication(APPLICATION_ID);
    }

    @Test
    void submitApplication_ShouldThrowAccessDenied_WhenBuyerDoesNotOwnApplication() {

        Application application =
                createApplication(
                        OTHER_USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        mockBuyerClaims(BUYER_TOKEN, USER_ID);

        when(applicationService.getApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(application)
        );

        StepVerifier.create(
                        applicationController.submitApplication(
                                APPLICATION_ID,
                                null,
                                null,
                                null,
                                BUYER_TOKEN,
                                null
                        )
                )
                .expectError(AccessDeniedException.class)
                .verify();

        verify(applicationService, never())
                .submitApplication(APPLICATION_ID);
    }

    // =========================================================
    // WITHDRAW APPLICATION TESTS
    // =========================================================

    @Test
    void withdrawApplication_ShouldWithdraw_WhenBuyerOwnsApplication() {

        Application application =
                createApplication(
                        USER_ID,
                        ApplicationStatus.SUBMITTED.name()
                );

        Application withdrawnApplication =
                createApplication(
                        USER_ID,
                        ApplicationStatus.WITHDRAWN.name()
                );

        mockBuyerClaims(BUYER_TOKEN, USER_ID);

        when(applicationService.getApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(application)
        );

        when(applicationService.withdrawApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(withdrawnApplication)
        );

        StepVerifier.create(
                        applicationController.withdrawApplication(
                                APPLICATION_ID,
                                null,
                                null,
                                null,
                                BUYER_TOKEN,
                                null
                        )
                )
                .assertNext(response ->
                        assertEquals(
                                ApplicationStatus.WITHDRAWN.name(),
                                response.getStatus()
                        )
                )
                .verifyComplete();

        verify(applicationService)
                .withdrawApplication(APPLICATION_ID);
    }

    @Test
    void withdrawApplication_ShouldThrowAccessDenied_WhenBuyerDoesNotOwnApplication() {

        Application application =
                createApplication(
                        OTHER_USER_ID,
                        ApplicationStatus.SUBMITTED.name()
                );

        mockBuyerClaims(BUYER_TOKEN, USER_ID);

        when(applicationService.getApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(application)
        );

        StepVerifier.create(
                        applicationController.withdrawApplication(
                                APPLICATION_ID,
                                null,
                                null,
                                null,
                                BUYER_TOKEN,
                                null
                        )
                )
                .expectError(AccessDeniedException.class)
                .verify();

        verify(applicationService, never())
                .withdrawApplication(APPLICATION_ID);
    }

    // =========================================================
    // JWT FAILURE TEST
    // =========================================================

    @Test
    void createApplication_ShouldPropagateException_WhenJwtIsInvalid() {

        CreateApplicationRequest request =
                createApplicationRequest();

        when(jwtService.extractUserId("invalid-token"))
                .thenThrow(
                        new IllegalArgumentException(
                                "Invalid JWT"
                        )
                );

        StepVerifier.create(
                        Mono.defer(() ->
                                applicationController
                                        .createApplication(
                                                null,
                                                null,
                                                null,
                                                "invalid-token",
                                                null,
                                                request
                                        )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof IllegalArgumentException
                                && error.getMessage().equals(
                                "Invalid JWT"
                        )
                )
                .verify();

        verify(applicationService, never())
                .createApplication(
                        any(),
                        any(CreateApplicationRequest.class)
                );
    }

    // =========================================================
    // TEST DATA AND MOCK HELPERS
    // =========================================================

    private void mockBuyerClaims(
            String token,
            Integer userId
    ) {
        when(jwtService.extractUserId(token))
                .thenReturn(userId);

        when(jwtService.extractRole(token))
                .thenReturn("BUYER");

        when(jwtService.extractEmail(token))
                .thenReturn("buyer@gmail.com");
    }

    private void mockAdminClaims(
            String token
    ) {
        when(jwtService.extractUserId(token))
                .thenReturn(1);

        when(jwtService.extractRole(token))
                .thenReturn("ADMIN");

        when(jwtService.extractEmail(token))
                .thenReturn("admin@gmail.com");
    }

    private CreateApplicationRequest createApplicationRequest() {

        CreateApplicationRequest request =
                new CreateApplicationRequest();

        request.setBuilderName(
                "Apexes Infrastructure"
        );

        request.setProjectName(
                "Skyline Heights"
        );

        request.setUnitNumber(
                "B-1404"
        );

        request.setPropertyValue(
                new BigDecimal("12500000.00")
        );

        request.setPurchaseDate(
                LocalDate.of(2024, 11, 10)
        );

        request.setPromisedPossessionDate(
                LocalDate.of(2026, 12, 31)
        );

        return request;
    }

    private UploadDocumentRequest createUploadDocumentRequest() {

        UploadDocumentRequest request =
                new UploadDocumentRequest();

        request.setDocumentType(
                "SALE_AGREEMENT"
        );

        request.setFileName(
                "sale-agreement.pdf"
        );

        request.setFilePath(
                "/documents/sale-agreement.pdf"
        );

        return request;
    }

    private Application createApplication(
            Integer userId,
            String status
    ) {
        Application application =
                new Application();

        application.setApplicationId(
                APPLICATION_ID
        );

        application.setUserId(userId);

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

    private ApplicationDocument createDocument() {

        ApplicationDocument document =
                new ApplicationDocument();

        document.setDocumentId(
                "document-101"
        );

        document.setApplicationId(
                APPLICATION_ID
        );

        document.setDocumentType(
                "SALE_AGREEMENT"
        );

        document.setFileName(
                "sale-agreement.pdf"
        );

        document.setFilePath(
                "/documents/sale-agreement.pdf"
        );

        document.setReviewStatus(
                "PENDING_REVIEW"
        );

        return document;
    }
}
