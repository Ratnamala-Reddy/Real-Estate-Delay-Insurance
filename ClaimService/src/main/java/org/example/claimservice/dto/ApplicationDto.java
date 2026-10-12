package org.example.claimservice.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ApplicationDto {

    private String applicationId;

    private Integer userId;

    private LocalDate promisedPossessionDate;

    private String status;
}
