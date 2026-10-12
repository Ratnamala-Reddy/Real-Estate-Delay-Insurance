package org.example.policyservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePolicyRequest {

    @NotBlank(
            message = "Quote ID is required"
    )
    private String quoteId;

    @NotBlank(
            message = "Payment ID is required"
    )
    private String paymentId;
}