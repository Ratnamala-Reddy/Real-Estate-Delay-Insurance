package org.example.paymentservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.paymentservice.dto.CompensationPaymentRequest;
import org.example.paymentservice.entity.CompensationPayment;
import org.example.paymentservice.service.PaymentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/compensation-payments")
@RequiredArgsConstructor
public class CompensationPaymentController {

    private final PaymentService paymentService;

    // INTERNAL ADMIN CALL AFTER CLAIM APPROVAL
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Mono<CompensationPayment> createCompensationPayment(
            @Valid @RequestBody
            CompensationPaymentRequest request
    ) {
        return paymentService.createCompensationPayment(
                request
        );
    }

    // BUYER OR ADMIN - LIST COMPENSATION PAYMENTS
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @GetMapping
    public reactor.core.publisher.Flux<CompensationPayment> getAllCompensationPayments(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.example.paymentservice.security.AuthenticatedUser caller,
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

        return paymentService.getAllCompensationPayments(userId, role);
    }

    public Mono<CompensationPayment> getCompensationPayment(String paymentId) {
        return paymentService.getCompensationPayment(paymentId);
    }

    // BUYER OWNER OR ADMIN
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @GetMapping("/{paymentId}")
    public Mono<CompensationPayment> getCompensationPayment(
            @PathVariable String paymentId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.example.paymentservice.security.AuthenticatedUser caller,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Role", required = false) String xUserRole
    ) {
        Integer userId = caller != null ? caller.userId() : parseUserId(xUserId);
        String role = caller != null ? caller.role() : xUserRole;

        if (role == null) {
            return Mono.error(
                    new org.springframework.security.access.AccessDeniedException("Caller identity missing")
            );
        }

        return paymentService.getCompensationPayment(paymentId, userId, role);
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
}