package org.example.paymentservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.paymentservice.dto.PremiumPaymentRequest;
import org.example.paymentservice.entity.PremiumPayment;
import org.example.paymentservice.security.AuthenticatedUser;
import org.example.paymentservice.service.PaymentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/premium-payments")
@RequiredArgsConstructor
public class PremiumPaymentController {

    private final PaymentService paymentService;

    // BUYER ONLY
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping
    public Mono<PremiumPayment> createPremiumPayment(
            @Valid @RequestBody
            PremiumPaymentRequest request,
            @AuthenticationPrincipal
            AuthenticatedUser caller
    ) {
        return paymentService.createPremiumPayment(
                request,
                caller.userId(),
                caller.role()
        );
    }

    // BUYER OR ADMIN (ADMIN SEES ALL, BUYER SEES OWN PAYMENTS)
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @GetMapping
    public reactor.core.publisher.Flux<PremiumPayment> getAllPremiumPayments(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Role", required = false) String xUserRole
    ) {
        Integer userId = caller != null ? caller.userId() : parseUserId(xUserId);
        String role = caller != null ? caller.role() : xUserRole;

        if (role == null) {
            return reactor.core.publisher.Flux.error(
                    new org.springframework.security.access.AccessDeniedException("Caller identity missing")
            );
        }

        return paymentService.getAllPremiumPayments(userId, role);
    }

    private Integer parseUserId(String userIdStr) {
        if (userIdStr == null || userIdStr.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(userIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // BUYER OWNER OR ADMIN
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @GetMapping("/{paymentId}")
    public Mono<PremiumPayment> getPremiumPayment(
            @PathVariable String paymentId,
            @AuthenticationPrincipal
            AuthenticatedUser caller
    ) {
        return paymentService.getPremiumPayment(
                paymentId,
                caller.userId(),
                caller.role()
        );
    }
}