package org.example.policyservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.policyservice.dto.GenerateQuoteRequest;
import org.example.policyservice.entity.Quote;
import org.example.policyservice.security.AuthenticatedUser;
import org.example.policyservice.service.PolicyService;
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
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final PolicyService policyService;

    // BUYER OR ADMIN
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @PostMapping("/applications/{applicationId}")
    public Flux<Quote> generateQuotes(
            @PathVariable String applicationId,
            @Valid @RequestBody
            GenerateQuoteRequest request
    ) {
        request.setApplicationId(
                applicationId
        );

        return policyService.generateQuotes(
                request
        );
    }

    // BUYER OR ADMIN
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @GetMapping("/applications/{applicationId}")
    public Flux<Quote> getQuotes(
            @PathVariable String applicationId,
            @AuthenticationPrincipal
            AuthenticatedUser caller
    ) {
        return policyService.getQuotes(
                applicationId,
                caller.userId(),
                caller.role()
        );
    }

    // BUYER OR ADMIN
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @GetMapping("/{quoteId}")
    public Mono<Quote> getQuote(
            @PathVariable String quoteId,
            @AuthenticationPrincipal
            AuthenticatedUser caller
    ) {
        return policyService.getQuote(
                quoteId,
                caller.userId(),
                caller.role()
        );
    }

    // BUYER ONLY
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping("/{quoteId}/accept")
    public Mono<Quote> acceptQuote(
            @PathVariable String quoteId,
            @AuthenticationPrincipal
            AuthenticatedUser caller
    ) {
        return policyService.acceptQuote(
                quoteId,
                caller.userId(),
                caller.role()
        );
    }

    // BUYER ONLY
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping("/{quoteId}/decline")
    public Mono<Quote> declineQuote(
            @PathVariable String quoteId,
            @AuthenticationPrincipal
            AuthenticatedUser caller
    ) {
        return policyService.declineQuote(
                quoteId,
                caller.userId(),
                caller.role()
        );
    }
}