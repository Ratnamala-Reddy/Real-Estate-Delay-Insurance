package org.example.verifyservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.verifyservice.dto.AdditionalInfoRequest;
import org.example.verifyservice.dto.ApplicationResponse;
import org.example.verifyservice.dto.ApprovalRequest;
import org.example.verifyservice.dto.PropertyVerificationRequest;
import org.example.verifyservice.dto.RejectVerificationRequest;
import org.example.verifyservice.entity.VerificationRecord;
import org.example.verifyservice.service.VerificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/verifications")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    // ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/applications")
    public Flux<ApplicationResponse> getVerificationQueue() {
        return verificationService.getVerificationQueue();
    }

    // ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(
            "/applications/{applicationId}/property-verification"
    )
    public Mono<VerificationRecord> verifyProperty(
            @PathVariable String applicationId,
            @Valid @RequestBody
            PropertyVerificationRequest request
    ) {
        return verificationService.verifyProperty(
                applicationId,
                request
        );
    }

    // ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(
            "/applications/{applicationId}/request-information"
    )
    public Mono<VerificationRecord> requestInformation(
            @PathVariable String applicationId,
            @Valid @RequestBody
            AdditionalInfoRequest request
    ) {
        return verificationService.requestInformation(
                applicationId,
                request
        );
    }

    // ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(
            "/applications/{applicationId}/approve"
    )
    public Mono<VerificationRecord> approveVerification(
            @PathVariable String applicationId,
            @Valid @RequestBody
            ApprovalRequest request
    ) {
        return verificationService.approveVerification(
                applicationId,
                request
        );
    }

    // ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(
            "/applications/{applicationId}/reject"
    )
    public Mono<VerificationRecord> rejectApplication(
            @PathVariable String applicationId,
            @Valid @RequestBody
            RejectVerificationRequest request
    ) {
        return verificationService.rejectApplication(
                applicationId,
                request
        );
    }

    // BUYER OR ADMIN
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @GetMapping("/applications/{applicationId}")
    public Mono<VerificationRecord> getVerificationResult(
            @PathVariable String applicationId
    ) {
        return verificationService.getVerificationResult(
                applicationId
        );
    }
}