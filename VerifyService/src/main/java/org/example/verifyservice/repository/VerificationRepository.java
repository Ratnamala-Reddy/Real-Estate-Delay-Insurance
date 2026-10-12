package org.example.verifyservice.repository;

import org.example.verifyservice.entity.VerificationRecord;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface VerificationRepository
        extends ReactiveCrudRepository<VerificationRecord, String> {

    Mono<VerificationRecord> findByApplicationId(String applicationId);

    Flux<VerificationRecord> findAllByApplicationIdOrderByVerifiedAtDesc(String applicationId);
}