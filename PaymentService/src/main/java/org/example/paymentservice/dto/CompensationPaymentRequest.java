package org.example.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompensationPaymentRequest {

    @NotBlank(
            message = "Claim ID is required"
    )
    private String claimId;

    @NotBlank(
            message = "Policy ID is required"
    )
    private String policyId;

    @NotNull(
            message = "Payout amount is required"
    )
    @DecimalMin(
            value = "0.01",
            message = "Payout amount must be greater than zero"
    )
    private Double payoutAmount;
}