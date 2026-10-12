package org.example.verifyservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ApplicationResponse {

    private String applicationId;

    private Integer userId;

    private String builderName;

    private String projectName;

    private String unitNumber;

    private BigDecimal propertyValue;

    private LocalDate purchaseDate;

    private LocalDate promisedPossessionDate;

    private String status;
}