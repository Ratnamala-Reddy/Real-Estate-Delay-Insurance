package org.example.paymentservice.repository;

import org.example.paymentservice.entity.PremiumPayment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PremiumPaymentRepository
        extends ReactiveCrudRepository<PremiumPayment, String> {

    Mono<PremiumPayment> findByQuoteId(
            String quoteId
    );

    Mono<Boolean> existsByQuoteId(
            String quoteId
    );

    Mono<Boolean> existsByTransactionReference(
            String transactionReference
    );
}