package org.example.applicationservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateApplicationRequest {

    private String builderName;

    private String projectName;

    private String unitNumber;

    private BigDecimal propertyValue;

    private LocalDate purchaseDate;

    private LocalDate promisedPossessionDate;
}