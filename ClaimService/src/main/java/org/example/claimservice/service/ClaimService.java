package org.example.claimservice.service;

import org.example.claimservice.dto.CreateClaimRequest;
import org.example.claimservice.entity.Claim;
import reactor.core.publisher.Mono;

public interface ClaimService {

    Mono<Claim> createClaim(
            CreateClaimRequest request,
            Integer callerUserId,
            String callerRole
    );

    reactor.core.publisher.Flux<Claim> getClaims();

    reactor.core.publisher.Flux<Claim> getClaims(
            Integer callerUserId,
            String callerRole
    );

    Mono<Claim> getClaim(
            String claimId
    );

    Mono<Claim> getClaim(
            String claimId,
            Integer callerUserId,
            String callerRole
    );

    Mono<Claim> calculateDelay(
            String claimId
    );

    Mono<Claim> calculateCompensation(
            String claimId
    );

    Mono<Claim> verifyInsuranceEligibility(
            String claimId
    );

    Mono<Claim> approveClaim(
            String claimId
    );

    Mono<Claim> rejectClaim(
            String claimId
    );

    Mono<Claim> closeClaim(
            String claimId
    );
}