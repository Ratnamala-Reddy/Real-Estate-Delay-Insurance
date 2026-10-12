package org.example.riskservice.dto;

import lombok.Data;

@Data
public class QuoteGenerationRequest {

    private String applicationId;

    private Double coverageAmount;

    private String generatedBy;
}
