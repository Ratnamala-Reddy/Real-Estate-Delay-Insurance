package org.example.policyservice.repository;

import org.example.policyservice.entity.Policy;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PolicyRepository
        extends ReactiveCrudRepository<Policy, String> {

    Mono<Policy> findByApplicationId(
            String applicationId
    );

    Mono<Policy> findByQuoteId(
            String quoteId
    );

    Mono<Boolean> existsByQuoteId(
            String quoteId
    );

    Mono<Policy> findByPolicyNumber(
            String policyNumber
    );
}