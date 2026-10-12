package org.example.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PremiumPaymentRequest {

    @NotBlank(
            message = "Quote ID is required"
    )
    private String quoteId;


    @NotBlank(
            message = "Payment provider is required"
    )
    private String paymentProvider;
}