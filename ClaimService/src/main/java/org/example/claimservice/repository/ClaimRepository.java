package org.example.claimservice.repository;

import org.example.claimservice.entity.Claim;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ClaimRepository
        extends ReactiveCrudRepository<Claim, String> {

    Mono<Boolean> existsByPolicyId(
            String policyId
    );

    Flux<Claim> findByPolicyId(
            String policyId
    );

    Flux<Claim> findByApplicationId(
            String applicationId
    );
}