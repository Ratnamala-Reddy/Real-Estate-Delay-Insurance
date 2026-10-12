package org.example.policyservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("policies")
public class Policy implements Persistable<String> {

    @Id
    @Column("policy_id")
    private String policyId;

    @Column("application_id")
    private String applicationId;

    @Column("quote_id")
    private String quoteId;

    @Column("payment_id")
    private String paymentId;

    @Column("policy_number")
    private String policyNumber;

    @Column("coverage_amount")
    private Double coverageAmount;

    @Column("premium_amount")
    private Double premiumAmount;

    @Column("policy_status")
    private String policyStatus;

    @Column("start_date")
    private LocalDate startDate;

    @Column("end_date")
    private LocalDate endDate;

    @Column("created_at")
    private LocalDateTime createdAt;



    @Transient
    private boolean isNew = true;

    @Override
    public String getId() { return policyId; }
    @Override
    public boolean isNew() { return isNew; }
    public void setNew(boolean isNew) { this.isNew = isNew; }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public String getQuoteId() { return quoteId; }
    public void setQuoteId(String quoteId) { this.quoteId = quoteId; }
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    public Double getCoverageAmount() { return coverageAmount; }
    public void setCoverageAmount(Double coverageAmount) { this.coverageAmount = coverageAmount; }
    public Double getPremiumAmount() { return premiumAmount; }
    public void setPremiumAmount(Double premiumAmount) { this.premiumAmount = premiumAmount; }
    public String getPolicyStatus() { return policyStatus; }
    public void setPolicyStatus(String policyStatus) { this.policyStatus = policyStatus; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}