package org.example.riskservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RiskAssessmentRequest {

    @NotBlank(
            message = "Application ID is required"
    )
    private String applicationId;

    @NotBlank(
            message = "Assessed by is required"
    )
    private String assessedBy;

    @Min(
            value = 0,
            message = "Builder score must be at least 0"
    )
    @Max(
            value = 100,
            message = "Builder score must not exceed 100"
    )
    private Integer builderScore;

    @Min(
            value = 0,
            message = "Financial score must be at least 0"
    )
    @Max(
            value = 100,
            message = "Financial score must not exceed 100"
    )
    private Integer financialScore;

    @Min(
            value = 0,
            message = "Project progress score must be at least 0"
    )
    @Max(
            value = 100,
            message = "Project progress score must not exceed 100"
    )
    private Integer projectProgressScore;

    @Min(
            value = 0,
            message = "Location score must be at least 0"
    )
    @Max(
            value = 100,
            message = "Location score must not exceed 100"
    )
    private Integer locationScore;

    @Min(
            value = 0,
            message = "Approval score must be at least 0"
    )
    @Max(
            value = 100,
            message = "Approval score must not exceed 100"
    )
    private Integer approvalScore;
}