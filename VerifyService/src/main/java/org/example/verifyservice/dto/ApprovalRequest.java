package org.example.verifyservice.dto;

import lombok.Data;

@Data
public class ApprovalRequest {

    private String remarks;

    private String approvedBy;
}