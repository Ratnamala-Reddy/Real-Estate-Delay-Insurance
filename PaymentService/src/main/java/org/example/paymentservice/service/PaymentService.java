package org.example.paymentservice.service;

import org.example.paymentservice.dto.CompensationPaymentRequest;
import org.example.paymentservice.dto.CompensationWebhookRequest;
import org.example.paymentservice.dto.PremiumPaymentRequest;
import org.example.paymentservice.dto.PremiumPaymentWebhookRequest;
import org.example.paymentservice.entity.CompensationPayment;
import org.example.paymentservice.entity.PremiumPayment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentService {

    Mono<PremiumPayment> createPremiumPayment(
            PremiumPaymentRequest request,
            Integer callerUserId,
            String callerRole
    );

    Mono<PremiumPayment> getPremiumPayment(
            String paymentId,
            Integer callerUserId,
            String callerRole
    );

    Flux<PremiumPayment> getAllPremiumPayments();

    Flux<PremiumPayment> getAllPremiumPayments(
            Integer callerUserId,
            String callerRole
    );

    Mono<PremiumPayment> processPremiumWebhook(
            PremiumPaymentWebhookRequest request
    );

    Mono<CompensationPayment> createCompensationPayment(
            CompensationPaymentRequest request
    );

    Mono<CompensationPayment> getCompensationPayment(
            String paymentId
    );

    Mono<CompensationPayment> getCompensationPayment(
            String paymentId,
            Integer callerUserId,
            String callerRole
    );

    Flux<CompensationPayment> getAllCompensationPayments();

    Flux<CompensationPayment> getAllCompensationPayments(
            Integer callerUserId,
            String callerRole
    );

    Mono<CompensationPayment> processCompensationWebhook(
            CompensationWebhookRequest request
    );
}