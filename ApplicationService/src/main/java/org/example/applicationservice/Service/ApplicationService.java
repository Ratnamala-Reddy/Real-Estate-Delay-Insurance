package org.example.applicationservice.Service;

import org.example.applicationservice.dto.CreateApplicationRequest;
import org.example.applicationservice.dto.UploadDocumentRequest;
import org.example.applicationservice.Entity.Application;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.example.applicationservice.Entity.ApplicationDocument;

public interface ApplicationService {

    Mono<Application> createApplication(
            Integer userId,
            CreateApplicationRequest request
    );
    Flux<Application> getMyApplications(
            Integer userId
    );

    Mono<Application> submitApplication(
            String applicationId
    );

    Mono<Application> withdrawApplication(
            String applicationId
    );


    Mono<Application> getApplication(
            String applicationId
    );

    Mono<Application> updateApplication(
            String applicationId,
            CreateApplicationRequest request
    );

    Mono<Application> updateApplicationStatus(
            String applicationId,
            String status
    );

    Mono<org.example.applicationservice.Entity.ApplicationDocument> uploadDocument(
            String applicationId,
            UploadDocumentRequest request
    );

    Flux<Application> getAllApplications();

    Flux<ApplicationDocument> getDocuments(
            String applicationId
    );
}