package org.example.applicationservice.Service;

import lombok.RequiredArgsConstructor;
import org.example.applicationservice.Entity.ApplicationDocument;
import org.example.applicationservice.Repository.ApplicationDocumentRepository;
import org.example.applicationservice.dto.CreateApplicationRequest;
import org.example.applicationservice.dto.UploadDocumentRequest;
import org.example.applicationservice.Entity.Application;
import org.example.applicationservice.enums.ApplicationStatus;
import org.example.applicationservice.exceptions.ApplicationAlreadyExistsException;
import org.example.applicationservice.exceptions.ApplicationCannotBeUpdatedException;
import org.example.applicationservice.exceptions.ApplicationNotFoundException;
import org.example.applicationservice.Repository.ApplicationRepository;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.example.applicationservice.exceptions.ApplicationCannotBeSubmittedException;
import org.example.applicationservice.exceptions.ApplicationCannotBeWithdrawnException;
import org.example.applicationservice.exceptions.InvalidApplicationStatusException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final R2dbcEntityTemplate template;

    @Override
    public Mono<Application> createApplication(
            Integer userId,
            CreateApplicationRequest request
    ) {

        return applicationRepository
                .findByUserIdAndProjectNameAndUnitNumber(
                        userId,
                        request.getProjectName(),
                        request.getUnitNumber()
                )
                .hasElement()
                .flatMap(exists -> {

                    if (exists) {
                        return Mono.error(
                                new ApplicationAlreadyExistsException(
                                        "Application already exists for the given project and unit"
                                )
                        );
                    }

                    Application application = new Application();

                    application.setApplicationId(
                            UUID.randomUUID().toString()
                    );

                    application.setUserId(userId);
                    application.setBuilderName(request.getBuilderName());
                    application.setProjectName(request.getProjectName());
                    application.setUnitNumber(request.getUnitNumber());
                    application.setPropertyValue(request.getPropertyValue());
                    application.setPurchaseDate(request.getPurchaseDate());
                    application.setPromisedPossessionDate(
                            request.getPromisedPossessionDate()
                    );
                    application.setStatus(
                            ApplicationStatus.DRAFT.name()
                    );

                    return template.insert(Application.class)
                            .using(application);
                });
    }

    @Override
    public Flux<Application> getMyApplications(
            Integer userId
    ) {

        return applicationRepository
                .findByUserId(userId);
    }

    @Override
    public Flux<Application> getAllApplications() {

        return applicationRepository.findAll();
    }

    @Override
    public Mono<Application> getApplication(
            String applicationId
    ) {

        return applicationRepository
                .findById(applicationId);
    }

    @Override
    public Mono<Application> updateApplication(
            String applicationId,
            CreateApplicationRequest request
    ) {

        return applicationRepository
                .findById(applicationId)
                .switchIfEmpty(
                        Mono.error(
                                new ApplicationNotFoundException(
                                        "Application not found with id: " + applicationId
                                )
                        )
                )
                .flatMap(application -> {

                    if ("SUBMITTED".equals(application.getStatus())) {
                        return Mono.error(
                                new ApplicationCannotBeUpdatedException(
                                        "Submitted applications cannot be updated"
                                )
                        );
                    }

                    if ("WITHDRAWN".equals(application.getStatus())) {
                        return Mono.error(
                                new ApplicationCannotBeUpdatedException(
                                        "Withdrawn applications cannot be updated"
                                )
                        );
                    }

                    application.setBuilderName(
                            request.getBuilderName()
                    );

                    application.setProjectName(
                            request.getProjectName()
                    );

                    application.setUnitNumber(
                            request.getUnitNumber()
                    );

                    application.setPropertyValue(
                            request.getPropertyValue()
                    );

                    application.setPurchaseDate(
                            request.getPurchaseDate()
                    );

                    application.setPromisedPossessionDate(
                            request.getPromisedPossessionDate()
                    );

                    return applicationRepository.save(
                            application
                    );
                });
    }

    @Override
    public Mono<ApplicationDocument> uploadDocument(
            String applicationId,
            UploadDocumentRequest request
    ) {

        ApplicationDocument document =
                new ApplicationDocument();

        document.setDocumentId(
                UUID.randomUUID().toString()
        );

        document.setApplicationId(
                applicationId
        );

        document.setDocumentType(
                request.getDocumentType()
        );

        document.setFileName(
                request.getFileName()
        );

        document.setFilePath(
                request.getFilePath()
        );

        document.setReviewStatus(
                "PENDING_REVIEW"
        );

        return template
                .insert(ApplicationDocument.class)
                .using(document);
    }

    @Override
    public Flux<ApplicationDocument> getDocuments(
            String applicationId
    ) {

        return applicationDocumentRepository
                .findByApplicationId(applicationId);
    }

    @Override
    public Mono<Application> updateApplicationStatus(
            String applicationId,
            String status
    ) {
        if (status == null || status.isBlank()) {
            return Mono.error(
                    new InvalidApplicationStatusException(
                            "Application status is required"
                    )
            );
        }

        ApplicationStatus applicationStatus;

        try {
            applicationStatus = ApplicationStatus.valueOf(
                    status.trim().toUpperCase()
            );
        } catch (IllegalArgumentException exception) {
            return Mono.error(
                    new InvalidApplicationStatusException(
                            "Invalid application status: " + status
                    )
            );
        }

        return applicationRepository
                .findById(applicationId)
                .switchIfEmpty(
                        Mono.error(
                                new ApplicationNotFoundException(
                                        "Application not found with id: "
                                                + applicationId
                                )
                        )
                )
                .flatMap(application -> {
                    application.setStatus(
                            applicationStatus.name()
                    );

                    return applicationRepository.save(application);
                });
    }
    @Override
    public Mono<Application> submitApplication(
            String applicationId
    ) {
        return applicationRepository
                .findById(applicationId)

                .switchIfEmpty(
                        Mono.error(
                                new ApplicationNotFoundException(
                                        "Application not found with id: "
                                                + applicationId
                                )
                        )
                )

                .flatMap(application -> {

                    if (!ApplicationStatus.DRAFT.name()
                            .equals(application.getStatus())) {

                        return Mono.error(
                                new ApplicationCannotBeSubmittedException(
                                        "Only DRAFT applications can be submitted"
                                )
                        );
                    }

                    return applicationDocumentRepository
                            .findByApplicationId(applicationId)
                            .collectList()

                            .flatMap(documents -> {

                                if (documents.isEmpty()) {
                                    return Mono.error(
                                            new ApplicationCannotBeSubmittedException(
                                                    "Upload documents before submission"
                                            )
                                    );
                                }

                                application.setStatus(
                                        ApplicationStatus.SUBMITTED.name()
                                );

                                return applicationRepository.save(
                                        application
                                );
                            });
                });
    }
    @Override
    public Mono<Application> withdrawApplication(
            String applicationId
    ) {
        return applicationRepository
                .findById(applicationId)

                .switchIfEmpty(
                        Mono.error(
                                new ApplicationNotFoundException(
                                        "Application not found with id: "
                                                + applicationId
                                )
                        )
                )

                .flatMap(application -> {

                    if (ApplicationStatus.VERIFIED.name()
                            .equals(application.getStatus())) {

                        return Mono.error(
                                new ApplicationCannotBeWithdrawnException(
                                        "Verified applications cannot be withdrawn"
                                )
                        );
                    }

                    if (ApplicationStatus.REJECTED.name()
                            .equals(application.getStatus())) {

                        return Mono.error(
                                new ApplicationCannotBeWithdrawnException(
                                        "Rejected applications cannot be withdrawn"
                                )
                        );
                    }

                    if (ApplicationStatus.WITHDRAWN.name()
                            .equals(application.getStatus())) {

                        return Mono.error(
                                new ApplicationCannotBeWithdrawnException(
                                        "Application already withdrawn"
                                )
                        );
                    }

                    application.setStatus(
                            ApplicationStatus.WITHDRAWN.name()
                    );

                    return applicationRepository.save(
                            application
                    );
                });
    }
}