package org.example.riskservice.repository;

import org.example.riskservice.entity.RiskAssessment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface RiskAssessmentRepository
        extends ReactiveCrudRepository<RiskAssessment, String> {

    Mono<RiskAssessment> findByApplicationId(
            String applicationId
    );
}