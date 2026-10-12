package org.example.policyservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RiskAssessmentDto {
    private String assessmentId;
    private String applicationId;
    private Integer riskScore;
    private String riskCategory;
    private Double premiumMultiplier;
    private String remarks;
    private String status;
    private String assessedBy;
    private LocalDateTime assessedAt;

    public String getAssessmentId() { return assessmentId; }
    public void setAssessmentId(String assessmentId) { this.assessmentId = assessmentId; }
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    public String getRiskCategory() { return riskCategory; }
    public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }
    public Double getPremiumMultiplier() { return premiumMultiplier; }
    public void setPremiumMultiplier(Double premiumMultiplier) { this.premiumMultiplier = premiumMultiplier; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAssessedBy() { return assessedBy; }
    public void setAssessedBy(String assessedBy) { this.assessedBy = assessedBy; }
    public LocalDateTime getAssessedAt() { return assessedAt; }
    public void setAssessedAt(LocalDateTime assessedAt) { this.assessedAt = assessedAt; }
}