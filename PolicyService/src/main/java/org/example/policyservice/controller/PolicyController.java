package org.example.policyservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.policyservice.dto.CreatePolicyRequest;
import org.example.policyservice.entity.Policy;
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
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

        private final PolicyService policyService;

        // BUYER OR ADMIN
        // INTERNAL CALL AFTER SUCCESSFUL PAYMENT
        @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
        @PostMapping
        public Mono<Policy> createPolicy(
                        @Valid @RequestBody CreatePolicyRequest request) {
                return policyService.createPolicy(
                                request);
        }

        // BUYER OR ADMIN
        @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
        @GetMapping
        public Flux<Policy> getPolicies(
                        @AuthenticationPrincipal AuthenticatedUser caller) {
                if (caller != null) {
                        return policyService.getPolicies(
                                        caller.userId(),
                                        caller.role());
                }
                return policyService.getPolicies();
        }

        // BUYER OR ADMIN
        @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
        @GetMapping("/{policyId}")
        public Mono<Policy> getPolicy(
                        @PathVariable String policyId,
                        @AuthenticationPrincipal AuthenticatedUser caller) {
                return policyService.getPolicy(
                                policyId,
                                caller.userId(),
                                caller.role());
        }

        // ADMIN ONLY
        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping("/{policyId}/complete")
        public Mono<Policy> completePolicy(
                        @PathVariable String policyId) {
                return policyService.completePolicy(
                                policyId);
        }

        // BUYER OR ADMIN - CANCELLATION / CLOSURE
        @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
        @PostMapping("/{policyId}/close")
        public Mono<Policy> closePolicy(
                        @PathVariable String policyId) {
                return policyService.closePolicy(
                                policyId);
        }
}