package org.example.paymentservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.paymentservice.dto.CompensationWebhookRequest;
import org.example.paymentservice.dto.PremiumPaymentWebhookRequest;
import org.example.paymentservice.entity.CompensationPayment;
import org.example.paymentservice.entity.PremiumPayment;
import org.example.paymentservice.service.PaymentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/payment-webhooks")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;

    // INTERNAL PAYMENT PROVIDER CALLBACK / SIMULATOR
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @PostMapping("/premium")
    public Mono<PremiumPayment> processPremiumWebhook(
            @Valid @RequestBody
            PremiumPaymentWebhookRequest request
    ) {
        return paymentService.processPremiumWebhook(
                request
        );
    }

    // INTERNAL PAYMENT PROVIDER CALLBACK / SIMULATOR
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @PostMapping("/compensation")
    public Mono<CompensationPayment> processCompensationWebhook(
            @Valid @RequestBody
            CompensationWebhookRequest request
    ) {
        return paymentService.processCompensationWebhook(
                request
        );
    }
}