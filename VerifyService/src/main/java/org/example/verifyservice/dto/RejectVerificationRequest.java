package org.example.verifyservice.dto;

import lombok.Data;

@Data
public class RejectVerificationRequest {

    private String remarks;

    private String rejectedBy;
}