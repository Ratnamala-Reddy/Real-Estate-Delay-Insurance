package org.example.claimservice.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PolicyDto {

    private String policyId;

    private String applicationId;

    private String quoteId;

    private String paymentId;

    private String policyNumber;

    private Double coverageAmount;

    private Double premiumAmount;

    private String policyStatus;

    private LocalDate startDate;

    private LocalDate endDate;
}
