package org.example.riskservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.riskservice.dto.ApproveRiskRequest;
import org.example.riskservice.dto.DeclineRiskRequest;
import org.example.riskservice.dto.RiskAssessmentRequest;
import org.example.riskservice.entity.RiskAssessment;
import org.example.riskservice.service.RiskService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/risk-assessments")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    // ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Mono<RiskAssessment> calculateRisk(
            @Valid @RequestBody
            RiskAssessmentRequest request
    ) {
        return riskService.calculateRisk(
                request
        );
    }

    // BUYER OR ADMIN
    @PreAuthorize(
            "hasAnyRole('BUYER', 'ADMIN')"
    )
    @GetMapping(
            "/applications/{applicationId}/risk-assessment"
    )
    public Mono<RiskAssessment> getRiskAssessment(
            @PathVariable String applicationId
    ) {
        return riskService.getRiskAssessment(
                applicationId
        );
    }

    // ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{assessmentId}/approve")
    public Mono<RiskAssessment> approveRiskAssessment(
            @PathVariable String assessmentId,
            @Valid @RequestBody
            ApproveRiskRequest request
    ) {
        return riskService.approveRiskAssessment(
                assessmentId,
                request
        );
    }

    // ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{assessmentId}/decline")
    public Mono<RiskAssessment> declineRiskAssessment(
            @PathVariable String assessmentId,
            @Valid @RequestBody
            DeclineRiskRequest request
    ) {
        return riskService.declineRiskAssessment(
                assessmentId,
                request
        );
    }
}