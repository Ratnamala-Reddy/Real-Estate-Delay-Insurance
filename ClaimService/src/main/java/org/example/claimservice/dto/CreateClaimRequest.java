package org.example.claimservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateClaimRequest {

    @NotBlank(
            message = "Policy ID is required"
    )
    private String policyId;

    /*
     * applicationId is intentionally removed.
     * Claim Service must read applicationId from
     * the trusted Policy Service response.
     */

    @NotBlank(
            message = "Claim reason is required"
    )
    private String reason;

    @NotNull(
            message = "Claimed amount is required"
    )
    @DecimalMin(
            value = "0.01",
            message = "Claimed amount must be greater than zero"
    )
    private Double claimedAmount;

    /*
     * Optional because a buyer may file a delay claim
     * before actual possession occurs.
     */
    private LocalDate actualPossessionDate;
}