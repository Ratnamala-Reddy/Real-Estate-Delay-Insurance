package org.example.verifyservice.dto;

import lombok.Data;

@Data
public class PropertyVerificationRequest {

    private String remarks;

    private String verifiedBy;
}