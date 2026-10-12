package org.example.paymentservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuoteDto {

    private String quoteId;

    private String applicationId;

    private Double premiumAmount;

    private String status;

    private LocalDateTime expiryDate;
}