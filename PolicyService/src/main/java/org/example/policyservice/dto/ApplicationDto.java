package org.example.policyservice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ApplicationDto {
    private String applicationId;
    private Integer userId;
    private String builderName;
    private String projectName;
    private String unitNumber;
    private BigDecimal propertyValue;
    private LocalDate purchaseDate;
    private LocalDate promisedPossessionDate;
    private String status;

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getBuilderName() { return builderName; }
    public void setBuilderName(String builderName) { this.builderName = builderName; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getUnitNumber() { return unitNumber; }
    public void setUnitNumber(String unitNumber) { this.unitNumber = unitNumber; }
    public BigDecimal getPropertyValue() { return propertyValue; }
    public void setPropertyValue(BigDecimal propertyValue) { this.propertyValue = propertyValue; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public LocalDate getPromisedPossessionDate() { return promisedPossessionDate; }
    public void setPromisedPossessionDate(LocalDate promisedPossessionDate) { this.promisedPossessionDate = promisedPossessionDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}