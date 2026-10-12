package org.example.verifyservice.dto;

import lombok.Data;

@Data
public class RiskAssessmentRequest {

    private String applicationId;

    private String assessedBy;

    private Integer builderScore;

    private Integer financialScore;

    private Integer projectProgressScore;

    private Integer locationScore;

    private Integer approvalScore;
}
