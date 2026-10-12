package org.example.applicationservice.Repository;

import org.example.applicationservice.Entity.*;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ApplicationDocumentRepository
        extends ReactiveCrudRepository<ApplicationDocument, String> {

    Flux<ApplicationDocument> findByApplicationId(
            String applicationId
    );
}