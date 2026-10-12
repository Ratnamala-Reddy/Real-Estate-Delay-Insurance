package org.example.claimservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.claimservice.dto.CreateClaimRequest;
import org.example.claimservice.entity.Claim;
import org.example.claimservice.security.AuthenticatedUser;
import org.example.claimservice.service.ClaimService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
public class ClaimController {

 private final ClaimService claimService;

 // BUYER ONLY
 @PreAuthorize("hasRole('BUYER')")
 @PostMapping
 public Mono<Claim> createClaim(
         @Valid @RequestBody
         CreateClaimRequest request,
         @AuthenticationPrincipal
         AuthenticatedUser caller
 ) {
  return claimService.createClaim(
          request,
          caller.userId(),
          caller.role()
  );
 }

 // BUYER OR ADMIN
 @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
 @GetMapping
 public reactor.core.publisher.Flux<Claim> getClaims(
         @AuthenticationPrincipal
         AuthenticatedUser caller,
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

  return claimService.getClaims(userId, role);
 }

 // BUYER OWNER OR ADMIN
 @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
 @GetMapping("/{claimId}")
 public Mono<Claim> getClaim(
         @PathVariable String claimId,
         @AuthenticationPrincipal
         AuthenticatedUser caller,
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

  return claimService.getClaim(
          claimId,
          userId,
          role
  );
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

 // ADMIN ONLY
 @PreAuthorize("hasRole('ADMIN')")
 @PostMapping("/{claimId}/delay-calculation")
 public Mono<Claim> calculateDelay(
         @PathVariable String claimId
 ) {
  return claimService.calculateDelay(
          claimId
  );
 }

 // ADMIN ONLY
 @PreAuthorize("hasRole('ADMIN')")
 @PostMapping("/{claimId}/compensation-calculation")
 public Mono<Claim> calculateCompensation(
         @PathVariable String claimId
 ) {
  return claimService.calculateCompensation(
          claimId
  );
 }

 // ADMIN ONLY
 @PreAuthorize("hasRole('ADMIN')")
 @PostMapping("/{claimId}/insurance-verification")
 public Mono<Claim> verifyInsuranceEligibility(
         @PathVariable String claimId
 ) {
  return claimService.verifyInsuranceEligibility(
          claimId
  );
 }

 // ADMIN ONLY
 @PreAuthorize("hasRole('ADMIN')")
 @PostMapping("/{claimId}/approve")
 public Mono<Claim> approveClaim(
         @PathVariable String claimId
 ) {
  return claimService.approveClaim(
          claimId
  );
 }

 // ADMIN ONLY
 @PreAuthorize("hasRole('ADMIN')")
 @PostMapping("/{claimId}/reject")
 public Mono<Claim> rejectClaim(
         @PathVariable String claimId
 ) {
  return claimService.rejectClaim(
          claimId
  );
 }

 // INTERNAL ADMIN CALL AFTER COMPENSATION PAYMENT
 @PreAuthorize("hasRole('ADMIN')")
 @PostMapping("/{claimId}/close")
 public Mono<Claim> closeClaim(
         @PathVariable String claimId
 ) {
  return claimService.closeClaim(
          claimId
  );
 }
}