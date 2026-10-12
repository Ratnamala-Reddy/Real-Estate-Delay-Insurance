package org.example.claimservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("claims")
public class Claim implements Persistable<String> {

    @Id
    @Column("claim_id")
    private String claimId;

    @Column("policy_id")
    private String policyId;

    @Column("application_id")
    private String applicationId;

    @Column("reason")
    private String reason;

    @Column("claimed_amount")
    private Double claimedAmount;

    @Column("approved_amount")
    private Double approvedAmount;

    @Column("eligible_delay_months")
    private Integer eligibleDelayMonths;

    @Column("status")
    private String status;

    @Column("remarks")
    private String remarks;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("actual_possession_date")
    private LocalDate actualPossessionDate;

    @Transient
    private boolean isNew = true;

    @Override
    public String getId() {
        return claimId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setNew(
            boolean isNew
    ) {
        this.isNew = isNew;
    }
}