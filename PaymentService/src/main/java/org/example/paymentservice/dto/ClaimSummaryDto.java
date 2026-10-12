package org.example.paymentservice.dto;

import lombok.Data;

@Data
public class ClaimSummaryDto {
    private String claimId;
    private String policyId;
    private String applicationId;
}

