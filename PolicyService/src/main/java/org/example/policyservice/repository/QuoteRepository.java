package org.example.policyservice.repository;

import org.example.policyservice.entity.Quote;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface QuoteRepository
        extends ReactiveCrudRepository<Quote, String> {

    Flux<Quote> findByApplicationId(
            String applicationId
    );

    Mono<Quote> findByQuoteId(
            String quoteId
    );

    Mono<Boolean> existsByApplicationId(
            String applicationId
    );

    Mono<Void> deleteByApplicationId(
            String applicationId
    );
}