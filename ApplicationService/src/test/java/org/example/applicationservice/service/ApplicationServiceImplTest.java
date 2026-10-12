package org.example.applicationservice.service;

import org.example.applicationservice.Service.ApplicationServiceImpl;
import org.example.applicationservice.dto.CreateApplicationRequest;
import org.example.applicationservice.dto.UploadDocumentRequest;
import org.example.applicationservice.Entity.Application;
import org.example.applicationservice.Entity.ApplicationDocument;
import org.example.applicationservice.enums.ApplicationStatus;
import org.example.applicationservice.exceptions.ApplicationAlreadyExistsException;
import org.example.applicationservice.exceptions.ApplicationCannotBeSubmittedException;
import org.example.applicationservice.exceptions.ApplicationCannotBeUpdatedException;
import org.example.applicationservice.exceptions.ApplicationCannotBeWithdrawnException;
import org.example.applicationservice.exceptions.ApplicationNotFoundException;
import org.example.applicationservice.exceptions.InvalidApplicationStatusException;
import org.example.applicationservice.Repository.ApplicationDocumentRepository;
import org.example.applicationservice.Repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationDocumentRepository applicationDocumentRepository;

    /*
     * Deep stubbing is used because R2dbcEntityTemplate.insert()
     * returns an intermediate fluent-operation object.
     */
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private R2dbcEntityTemplate template;

    private ApplicationServiceImpl applicationService;

    private static final Integer USER_ID = 4;

    private static final String APPLICATION_ID =
            "application-101";

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationServiceImpl(
                applicationRepository,
                applicationDocumentRepository,
                template
        );
    }

    // =========================================================
    // CREATE APPLICATION TESTS
    // =========================================================

    @Test
    void createApplication_ShouldCreateDraftApplication_WhenApplicationIsNew() {

        CreateApplicationRequest request =
                createApplicationRequest();

        when(applicationRepository
                .findByUserIdAndProjectNameAndUnitNumber(
                        USER_ID,
                        request.getProjectName(),
                        request.getUnitNumber()
                ))
                .thenReturn(Mono.empty());

        when(template
                .insert(Application.class)
                .using(any(Application.class)))
                .thenAnswer(invocation -> {
                    Application application =
                            invocation.getArgument(0);

                    return Mono.just(application);
                });

        StepVerifier.create(
                        applicationService.createApplication(
                                USER_ID,
                                request
                        )
                )
                .assertNext(application -> {
                    assertNotNull(
                            application.getApplicationId()
                    );

                    assertEquals(
                            USER_ID,
                            application.getUserId()
                    );

                    assertEquals(
                            "Apexes Infrastructure",
                            application.getBuilderName()
                    );

                    assertEquals(
                            "Skyline Heights",
                            application.getProjectName()
                    );

                    assertEquals(
                            "B-1404",
                            application.getUnitNumber()
                    );

                    assertEquals(
                            new BigDecimal("12500000.00"),
                            application.getPropertyValue()
                    );

                    assertEquals(
                            LocalDate.of(2024, 11, 10),
                            application.getPurchaseDate()
                    );

                    assertEquals(
                            LocalDate.of(2026, 12, 31),
                            application.getPromisedPossessionDate()
                    );

                    assertEquals(
                            ApplicationStatus.DRAFT.name(),
                            application.getStatus()
                    );
                })
                .verifyComplete();

        verify(applicationRepository)
                .findByUserIdAndProjectNameAndUnitNumber(
                        USER_ID,
                        request.getProjectName(),
                        request.getUnitNumber()
                );

    }

    @Test
    void createApplication_ShouldThrowException_WhenApplicationAlreadyExists() {

        CreateApplicationRequest request =
                createApplicationRequest();

        Application existingApplication =
                createApplication(
                        ApplicationStatus.DRAFT.name()
                );

        when(applicationRepository
                .findByUserIdAndProjectNameAndUnitNumber(
                        USER_ID,
                        request.getProjectName(),
                        request.getUnitNumber()
                ))
                .thenReturn(
                        Mono.just(existingApplication)
                );

        StepVerifier.create(
                        applicationService.createApplication(
                                USER_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ApplicationAlreadyExistsException
                                && error.getMessage().equals(
                                "Application already exists for the given project and unit"
                        )
                )
                .verify();

        verify(template, never())
                .insert(Application.class);
    }

    // =========================================================
    // GET MY APPLICATIONS TESTS
    // =========================================================

    @Test
    void getMyApplications_ShouldReturnApplicationsForUser() {

        Application first =
                createApplication(
                        ApplicationStatus.DRAFT.name()
                );

        Application second =
                createApplication(
                        ApplicationStatus.SUBMITTED.name()
                );

        second.setApplicationId("application-102");

        when(applicationRepository.findByUserId(USER_ID))
                .thenReturn(
                        Flux.just(first, second)
                );

        StepVerifier.create(
                        applicationService.getMyApplications(
                                USER_ID
                        )
                )
                .expectNext(first)
                .expectNext(second)
                .verifyComplete();

        verify(applicationRepository)
                .findByUserId(USER_ID);
    }

    @Test
    void getMyApplications_ShouldReturnEmptyFlux_WhenUserHasNoApplications() {

        when(applicationRepository.findByUserId(USER_ID))
                .thenReturn(Flux.empty());

        StepVerifier.create(
                        applicationService.getMyApplications(
                                USER_ID
                        )
                )
                .verifyComplete();

        verify(applicationRepository)
                .findByUserId(USER_ID);
    }

    // =========================================================
    // GET ALL APPLICATIONS TESTS
    // =========================================================

    @Test
    void getAllApplications_ShouldReturnAllApplications() {

        Application first =
                createApplication(
                        ApplicationStatus.DRAFT.name()
                );

        Application second =
                createApplication(
                        ApplicationStatus.SUBMITTED.name()
                );

        second.setApplicationId("application-102");
        second.setUserId(5);

        when(applicationRepository.findAll())
                .thenReturn(
                        Flux.just(first, second)
                );

        StepVerifier.create(
                        applicationService.getAllApplications()
                )
                .expectNext(first)
                .expectNext(second)
                .verifyComplete();

        verify(applicationRepository)
                .findAll();
    }

    @Test
    void getAllApplications_ShouldReturnEmptyFlux_WhenNoApplicationsExist() {

        when(applicationRepository.findAll())
                .thenReturn(Flux.empty());

        StepVerifier.create(
                        applicationService.getAllApplications()
                )
                .verifyComplete();

        verify(applicationRepository)
                .findAll();
    }

    // =========================================================
    // GET APPLICATION TESTS
    // =========================================================

    @Test
    void getApplication_ShouldReturnApplication_WhenApplicationExists() {

        Application application =
                createApplication(
                        ApplicationStatus.DRAFT.name()
                );

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.just(application));

        StepVerifier.create(
                        applicationService.getApplication(
                                APPLICATION_ID
                        )
                )
                .expectNext(application)
                .verifyComplete();

        verify(applicationRepository)
                .findById(APPLICATION_ID);
    }

    @Test
    void getApplication_ShouldReturnEmptyMono_WhenApplicationDoesNotExist() {

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        applicationService.getApplication(
                                APPLICATION_ID
                        )
                )
                .verifyComplete();

        verify(applicationRepository)
                .findById(APPLICATION_ID);
    }

    // =========================================================
    // UPDATE APPLICATION TESTS
    // =========================================================

    @Test
    void updateApplication_ShouldUpdateApplication_WhenStatusIsDraft() {

        Application application =
                createApplication(
                        ApplicationStatus.DRAFT.name()
                );

        CreateApplicationRequest request =
                createUpdatedApplicationRequest();

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.just(application));

        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation ->
                        Mono.just(invocation.getArgument(0))
                );

        StepVerifier.create(
                        applicationService.updateApplication(
                                APPLICATION_ID,
                                request
                        )
                )
                .assertNext(updatedApplication -> {
                    assertEquals(
                            "Updated Builders",
                            updatedApplication.getBuilderName()
                    );

                    assertEquals(
                            "Updated Heights",
                            updatedApplication.getProjectName()
                    );

                    assertEquals(
                            "C-1801",
                            updatedApplication.getUnitNumber()
                    );

                    assertEquals(
                            new BigDecimal("15000000.00"),
                            updatedApplication.getPropertyValue()
                    );

                    assertEquals(
                            LocalDate.of(2024, 12, 1),
                            updatedApplication.getPurchaseDate()
                    );

                    assertEquals(
                            LocalDate.of(2027, 1, 31),
                            updatedApplication
                                    .getPromisedPossessionDate()
                    );

                    assertEquals(
                            ApplicationStatus.DRAFT.name(),
                            updatedApplication.getStatus()
                    );
                })
                .verifyComplete();

        verify(applicationRepository)
                .findById(APPLICATION_ID);

        verify(applicationRepository)
                .save(application);
    }

    @Test
    void updateApplication_ShouldThrowNotFound_WhenApplicationDoesNotExist() {

        CreateApplicationRequest request =
                createUpdatedApplicationRequest();

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        applicationService.updateApplication(
                                APPLICATION_ID,
                                request
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

        verify(applicationRepository, never())
                .save(any(Application.class));
    }

    @Test
    void updateApplication_ShouldThrowException_WhenApplicationIsSubmitted() {

        Application application =
                createApplication(
                        ApplicationStatus.SUBMITTED.name()
                );

        CreateApplicationRequest request =
                createUpdatedApplicationRequest();

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.just(application));

        StepVerifier.create(
                        applicationService.updateApplication(
                                APPLICATION_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ApplicationCannotBeUpdatedException
                                && error.getMessage().equals(
                                "Submitted applications cannot be updated"
                        )
                )
                .verify();

        verify(applicationRepository, never())
                .save(any(Application.class));
    }

    @Test
    void updateApplication_ShouldThrowException_WhenApplicationIsWithdrawn() {

        Application application =
                createApplication(
                        ApplicationStatus.WITHDRAWN.name()
                );

        CreateApplicationRequest request =
                createUpdatedApplicationRequest();

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.just(application));

        StepVerifier.create(
                        applicationService.updateApplication(
                                APPLICATION_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ApplicationCannotBeUpdatedException
                                && error.getMessage().equals(
                                "Withdrawn applications cannot be updated"
                        )
                )
                .verify();

        verify(applicationRepository, never())
                .save(any(Application.class));
    }

    // =========================================================
    // UPLOAD DOCUMENT TESTS
    // =========================================================

    @Test
    void uploadDocument_ShouldCreatePendingDocument() {

        UploadDocumentRequest request =
                createUploadDocumentRequest();

        when(template
                .insert(ApplicationDocument.class)
                .using(any(ApplicationDocument.class)))
                .thenAnswer(invocation -> {
                    ApplicationDocument document =
                            invocation.getArgument(0);

                    return Mono.just(document);
                });

        StepVerifier.create(
                        applicationService.uploadDocument(
                                APPLICATION_ID,
                                request
                        )
                )
                .assertNext(document -> {
                    assertNotNull(
                            document.getDocumentId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            document.getApplicationId()
                    );

                    assertEquals(
                            "SALE_AGREEMENT",
                            document.getDocumentType()
                    );

                    assertEquals(
                            "sale-agreement.pdf",
                            document.getFileName()
                    );

                    assertEquals(
                            "/documents/sale-agreement.pdf",
                            document.getFilePath()
                    );

                    assertEquals(
                            "PENDING_REVIEW",
                            document.getReviewStatus()
                    );
                })
                .verifyComplete();


    }

    // =========================================================
    // GET DOCUMENTS TESTS
    // =========================================================

    @Test
    void getDocuments_ShouldReturnApplicationDocuments() {

        ApplicationDocument first =
                createDocument();

        ApplicationDocument second =
                createDocument();

        second.setDocumentId("document-102");
        second.setDocumentType("PAYMENT_RECEIPT");
        second.setFileName("payment-receipt.pdf");

        when(applicationDocumentRepository
                .findByApplicationId(APPLICATION_ID))
                .thenReturn(
                        Flux.just(first, second)
                );

        StepVerifier.create(
                        applicationService.getDocuments(
                                APPLICATION_ID
                        )
                )
                .expectNext(first)
                .expectNext(second)
                .verifyComplete();

        verify(applicationDocumentRepository)
                .findByApplicationId(APPLICATION_ID);
    }

    @Test
    void getDocuments_ShouldReturnEmptyFlux_WhenNoDocumentsExist() {

        when(applicationDocumentRepository
                .findByApplicationId(APPLICATION_ID))
                .thenReturn(Flux.empty());

        StepVerifier.create(
                        applicationService.getDocuments(
                                APPLICATION_ID
                        )
                )
                .verifyComplete();

        verify(applicationDocumentRepository)
                .findByApplicationId(APPLICATION_ID);
    }

    // =========================================================
    // UPDATE STATUS TESTS
    // =========================================================

    @Test
    void updateApplicationStatus_ShouldUpdateValidStatus() {

        Application application =
                createApplication(
                        ApplicationStatus.SUBMITTED.name()
                );

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.just(application));

        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation ->
                        Mono.just(invocation.getArgument(0))
                );

        StepVerifier.create(
                        applicationService.updateApplicationStatus(
                                APPLICATION_ID,
                                "VERIFICATION_IN_PROGRESS"
                        )
                )
                .assertNext(updatedApplication ->
                        assertEquals(
                                ApplicationStatus
                                        .VERIFICATION_IN_PROGRESS
                                        .name(),
                                updatedApplication.getStatus()
                        )
                )
                .verifyComplete();

        verify(applicationRepository)
                .findById(APPLICATION_ID);

        verify(applicationRepository)
                .save(application);
    }

    @Test
    void updateApplicationStatus_ShouldAcceptLowercaseValidStatus() {

        Application application =
                createApplication(
                        ApplicationStatus.SUBMITTED.name()
                );

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.just(application));

        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation ->
                        Mono.just(invocation.getArgument(0))
                );

        StepVerifier.create(
                        applicationService.updateApplicationStatus(
                                APPLICATION_ID,
                                "verified"
                        )
                )
                .assertNext(updatedApplication ->
                        assertEquals(
                                ApplicationStatus.VERIFIED.name(),
                                updatedApplication.getStatus()
                        )
                )
                .verifyComplete();

        verify(applicationRepository)
                .findById(APPLICATION_ID);

        verify(applicationRepository)
                .save(application);
    }

    @Test
    void updateApplicationStatus_ShouldThrowException_WhenStatusIsNull() {

        StepVerifier.create(
                        applicationService.updateApplicationStatus(
                                APPLICATION_ID,
                                null
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidApplicationStatusException
                                && error.getMessage().equals(
                                "Application status is required"
                        )
                )
                .verify();

        verify(applicationRepository, never())
                .findById(anyString());

        verify(applicationRepository, never())
                .save(any(Application.class));
    }

    @Test
    void updateApplicationStatus_ShouldThrowException_WhenStatusIsBlank() {

        StepVerifier.create(
                        applicationService.updateApplicationStatus(
                                APPLICATION_ID,
                                "   "
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidApplicationStatusException
                                && error.getMessage().equals(
                                "Application status is required"
                        )
                )
                .verify();

        verify(applicationRepository, never())
                .findById(anyString());

        verify(applicationRepository, never())
                .save(any(Application.class));
    }

    @Test
    void updateApplicationStatus_ShouldThrowException_WhenStatusIsInvalid() {

        StepVerifier.create(
                        applicationService.updateApplicationStatus(
                                APPLICATION_ID,
                                "HELLO"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidApplicationStatusException
                                && error.getMessage().equals(
                                "Invalid application status: HELLO"
                        )
                )
                .verify();

        verify(applicationRepository, never())
                .findById(anyString());

        verify(applicationRepository, never())
                .save(any(Application.class));
    }

    @Test
    void updateApplicationStatus_ShouldThrowNotFound_WhenApplicationDoesNotExist() {

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        applicationService.updateApplicationStatus(
                                APPLICATION_ID,
                                "VERIFIED"
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

        verify(applicationRepository, never())
                .save(any(Application.class));
    }

    // =========================================================
    // SUBMIT APPLICATION TESTS
    // =========================================================

    @Test
    void submitApplication_ShouldSubmitDraftApplication_WhenDocumentsExist() {

        Application application =
                createApplication(
                        ApplicationStatus.DRAFT.name()
                );

        ApplicationDocument document =
                createDocument();

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.just(application));

        when(applicationDocumentRepository
                .findByApplicationId(APPLICATION_ID))
                .thenReturn(
                        Flux.just(document)
                );

        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation ->
                        Mono.just(invocation.getArgument(0))
                );

        StepVerifier.create(
                        applicationService.submitApplication(
                                APPLICATION_ID
                        )
                )
                .assertNext(submittedApplication ->
                        assertEquals(
                                ApplicationStatus.SUBMITTED.name(),
                                submittedApplication.getStatus()
                        )
                )
                .verifyComplete();

        verify(applicationRepository)
                .findById(APPLICATION_ID);

        verify(applicationDocumentRepository)
                .findByApplicationId(APPLICATION_ID);

        verify(applicationRepository)
                .save(application);
    }

    @Test
    void submitApplication_ShouldThrowNotFound_WhenApplicationDoesNotExist() {

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        applicationService.submitApplication(
                                APPLICATION_ID
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

        verify(applicationDocumentRepository, never())
                .findByApplicationId(anyString());

        verify(applicationRepository, never())
                .save(any(Application.class));
    }

    @Test
    void submitApplication_ShouldThrowException_WhenStatusIsNotDraft() {

        Application application =
                createApplication(
                        ApplicationStatus.SUBMITTED.name()
                );

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.just(application));

        StepVerifier.create(
                        applicationService.submitApplication(
                                APPLICATION_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ApplicationCannotBeSubmittedException
                                && error.getMessage().equals(
                                "Only DRAFT applications can be submitted"
                        )
                )
                .verify();

        verify(applicationDocumentRepository, never())
                .findByApplicationId(anyString());

        verify(applicationRepository, never())
                .save(any(Application.class));
    }

    @Test
    void submitApplication_ShouldThrowException_WhenNoDocumentsExist() {

        Application application =
                createApplication(
                        ApplicationStatus.DRAFT.name()
                );

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.just(application));

        when(applicationDocumentRepository
                .findByApplicationId(APPLICATION_ID))
                .thenReturn(Flux.empty());

        StepVerifier.create(
                        applicationService.submitApplication(
                                APPLICATION_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ApplicationCannotBeSubmittedException
                                && error.getMessage().equals(
                                "Upload documents before submission"
                        )
                )
                .verify();

        verify(applicationDocumentRepository)
                .findByApplicationId(APPLICATION_ID);

        verify(applicationRepository, never())
                .save(any(Application.class));
    }

    // =========================================================
    // WITHDRAW APPLICATION TESTS
    // =========================================================

    @Test
    void withdrawApplication_ShouldWithdrawDraftApplication() {

        Application application =
                createApplication(
                        ApplicationStatus.DRAFT.name()
                );

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.just(application));

        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation ->
                        Mono.just(invocation.getArgument(0))
                );

        StepVerifier.create(
                        applicationService.withdrawApplication(
                                APPLICATION_ID
                        )
                )
                .assertNext(withdrawnApplication ->
                        assertEquals(
                                ApplicationStatus.WITHDRAWN.name(),
                                withdrawnApplication.getStatus()
                        )
                )
                .verifyComplete();

        verify(applicationRepository)
                .findById(APPLICATION_ID);

        verify(applicationRepository)
                .save(application);
    }

    @Test
    void withdrawApplication_ShouldThrowNotFound_WhenApplicationDoesNotExist() {

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        applicationService.withdrawApplication(
                                APPLICATION_ID
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

        verify(applicationRepository, never())
                .save(any(Application.class));
    }

    @Test
    void withdrawApplication_ShouldThrowException_WhenApplicationIsVerified() {

        Application application =
                createApplication(
                        ApplicationStatus.VERIFIED.name()
                );

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.just(application));

        StepVerifier.create(
                        applicationService.withdrawApplication(
                                APPLICATION_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ApplicationCannotBeWithdrawnException
                                && error.getMessage().equals(
                                "Verified applications cannot be withdrawn"
                        )
                )
                .verify();

        verify(applicationRepository, never())
                .save(any(Application.class));
    }

    @Test
    void withdrawApplication_ShouldThrowException_WhenApplicationIsRejected() {

        Application application =
                createApplication(
                        ApplicationStatus.REJECTED.name()
                );

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.just(application));

        StepVerifier.create(
                        applicationService.withdrawApplication(
                                APPLICATION_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ApplicationCannotBeWithdrawnException
                                && error.getMessage().equals(
                                "Rejected applications cannot be withdrawn"
                        )
                )
                .verify();

        verify(applicationRepository, never())
                .save(any(Application.class));
    }

    @Test
    void withdrawApplication_ShouldThrowException_WhenAlreadyWithdrawn() {

        Application application =
                createApplication(
                        ApplicationStatus.WITHDRAWN.name()
                );

        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Mono.just(application));

        StepVerifier.create(
                        applicationService.withdrawApplication(
                                APPLICATION_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ApplicationCannotBeWithdrawnException
                                && error.getMessage().equals(
                                "Application already withdrawn"
                        )
                )
                .verify();

        verify(applicationRepository, never())
                .save(any(Application.class));
    }

    // =========================================================
    // TEST DATA METHODS
    // =========================================================

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

    private CreateApplicationRequest createUpdatedApplicationRequest() {

        CreateApplicationRequest request =
                new CreateApplicationRequest();

        request.setBuilderName(
                "Updated Builders"
        );

        request.setProjectName(
                "Updated Heights"
        );

        request.setUnitNumber(
                "C-1801"
        );

        request.setPropertyValue(
                new BigDecimal("15000000.00")
        );

        request.setPurchaseDate(
                LocalDate.of(2024, 12, 1)
        );

        request.setPromisedPossessionDate(
                LocalDate.of(2027, 1, 31)
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
            String status
    ) {

        Application application =
                new Application();

        application.setApplicationId(
                APPLICATION_ID
        );

        application.setUserId(
                USER_ID
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