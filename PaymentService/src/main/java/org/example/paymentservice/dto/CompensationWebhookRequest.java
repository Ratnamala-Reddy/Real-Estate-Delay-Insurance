package org.example.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompensationWebhookRequest {

    @NotBlank(
            message = "Payment ID is required"
    )
    private String paymentId;

    @NotBlank(
            message = "Transaction reference is required"
    )
    private String transactionReference;

    @NotBlank(
            message = "Payment status is required"
    )
    private String paymentStatus;
}