package org.example.verifyservice.service;

import org.example.verifyservice.dto.*;
import org.example.verifyservice.entity.VerificationRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface VerificationService {

    Flux<ApplicationResponse> getVerificationQueue();

    Mono<VerificationRecord> verifyProperty(String applicationId, PropertyVerificationRequest request);

    Mono<VerificationRecord> requestInformation(String applicationId, AdditionalInfoRequest request);

    Mono<VerificationRecord> approveVerification(String applicationId, ApprovalRequest request);

    Mono<VerificationRecord> rejectApplication(String applicationId, RejectVerificationRequest request);

    Mono<VerificationRecord> getVerificationResult(String applicationId);

}