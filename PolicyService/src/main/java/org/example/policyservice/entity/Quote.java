package org.example.policyservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("quotes")
public class Quote implements Persistable<String> {

    @Id
    @Column("quote_id")
    private String quoteId;

    @Column("application_id")
    private String applicationId;

    @Column("plan_name")
    private String planName;

    @Column("premium_amount")
    private Double premiumAmount;

    @Column("monthly_compensation")
    private Double monthlyCompensation;

    @Column("maximum_delay_months")
    private Integer maximumDelayMonths;

    @Column("maximum_payout")
    private Double maximumPayout;

    @Column("grace_period_days")
    private Integer gracePeriodDays;

    @Column("expiry_date")
    private LocalDateTime expiryDate;

    @Column("status")
    private String status;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Transient
    private boolean isNew = true;

    @Override
    public String getId() { return quoteId; }
    @Override
    public boolean isNew() { return isNew; }
    public void setNew(boolean isNew) { this.isNew = isNew; }

    public String getQuoteId() { return quoteId; }
    public void setQuoteId(String quoteId) { this.quoteId = quoteId; }
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public Double getPremiumAmount() { return premiumAmount; }
    public void setPremiumAmount(Double premiumAmount) { this.premiumAmount = premiumAmount; }
    public Double getMonthlyCompensation() { return monthlyCompensation; }
    public void setMonthlyCompensation(Double monthlyCompensation) { this.monthlyCompensation = monthlyCompensation; }
    public Integer getMaximumDelayMonths() { return maximumDelayMonths; }
    public void setMaximumDelayMonths(Integer maximumDelayMonths) { this.maximumDelayMonths = maximumDelayMonths; }
    public Double getMaximumPayout() { return maximumPayout; }
    public void setMaximumPayout(Double maximumPayout) { this.maximumPayout = maximumPayout; }
    public Integer getGracePeriodDays() { return gracePeriodDays; }
    public void setGracePeriodDays(Integer gracePeriodDays) { this.gracePeriodDays = gracePeriodDays; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}