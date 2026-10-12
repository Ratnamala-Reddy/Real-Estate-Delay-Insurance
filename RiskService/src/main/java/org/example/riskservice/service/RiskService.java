package org.example.riskservice.service;

import org.example.riskservice.dto.ApproveRiskRequest;
import org.example.riskservice.dto.DeclineRiskRequest;
import org.example.riskservice.entity.RiskAssessment;
import reactor.core.publisher.Mono;
import org.example.riskservice.dto.RiskAssessmentRequest;

public interface RiskService {

    Mono<RiskAssessment> calculateRisk(RiskAssessmentRequest request);

    Mono<RiskAssessment> getRiskAssessment(String applicationId);

    Mono<RiskAssessment> approveRiskAssessment(String assessmentId, ApproveRiskRequest request);

    Mono<RiskAssessment> declineRiskAssessment(String assessmentId, DeclineRiskRequest request);


}
