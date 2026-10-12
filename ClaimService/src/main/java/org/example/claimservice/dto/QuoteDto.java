package org.example.claimservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuoteDto {

    private String quoteId;

    private String applicationId;

    private String planName;

    private Double premiumAmount;

    private Double monthlyCompensation;

    private Integer maximumDelayMonths;

    private Double maximumPayout;

    private Integer gracePeriodDays;

    private LocalDateTime expiryDate;

    private String status;
}
