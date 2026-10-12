package org.example.riskservice.dto;

import lombok.Data;

@Data
public class DeclineRiskRequest {

    private String remarks;

    private String declinedBy;
}