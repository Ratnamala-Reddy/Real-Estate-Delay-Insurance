package org.example.policyservice.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

@Data
public class GenerateQuoteRequest {

    private String applicationId;

    @DecimalMin(
            value = "1.0",
            message = "Coverage amount must be greater than zero"
    )
    private Double coverageAmount;

    private Double baseCoverageAmount;

    private String generatedBy;
}
