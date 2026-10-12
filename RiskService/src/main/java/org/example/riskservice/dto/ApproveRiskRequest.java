package org.example.riskservice.dto;

import lombok.Data;

@Data
public class ApproveRiskRequest {

    private String remarks;

    private String approvedBy;
}