package org.example.policyservice.service;

import org.example.policyservice.dto.CreatePolicyRequest;
import org.example.policyservice.dto.GenerateQuoteRequest;
import org.example.policyservice.entity.Policy;
import org.example.policyservice.entity.Quote;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PolicyService {

    Flux<Quote> generateQuotes(
            GenerateQuoteRequest request
    );

    Mono<Quote> generateQuote(
            GenerateQuoteRequest request
    );

    Flux<Quote> getQuotes(
            String applicationId
    );

    Flux<Quote> getQuotes(
            String applicationId,
            Integer callerUserId,
            String callerRole
    );

    Mono<Quote> getQuote(
            String quoteId
    );

    Mono<Quote> getQuote(
            String quoteId,
            Integer callerUserId,
            String callerRole
    );

    Mono<Quote> acceptQuote(
            String quoteId
    );

    Mono<Quote> acceptQuote(
            String quoteId,
            Integer callerUserId,
            String callerRole
    );

    Mono<Quote> declineQuote(
            String quoteId
    );

    Mono<Quote> declineQuote(
            String quoteId,
            Integer callerUserId,
            String callerRole
    );

    Mono<Policy> createPolicy(
            CreatePolicyRequest request
    );

    Mono<Policy> createPolicy(
            CreatePolicyRequest request,
            Integer callerUserId,
            String callerRole
    );

    Flux<Policy> getPolicies();

    Flux<Policy> getPolicies(
            Integer callerUserId,
            String callerRole
    );

    Mono<Policy> getPolicy(
            String policyId
    );

    Mono<Policy> getPolicy(
            String policyId,
            Integer callerUserId,
            String callerRole
    );

    Mono<Policy> completePolicy(
            String policyId
    );

    Mono<Policy> closePolicy(
            String policyId
    );
}