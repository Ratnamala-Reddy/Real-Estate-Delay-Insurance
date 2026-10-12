package org.example.riskservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("risk_assessments")
public class RiskAssessment implements Persistable<String> {

    @Id
    @Column("assessment_id")
    private String assessmentId;

    @Column("application_id")
    private String applicationId;

    @Column("risk_score")
    private Integer riskScore;

    @Column("risk_category")
    private String riskCategory;

    @Column("premium_multiplier")
    private Double premiumMultiplier;

    @Column("remarks")
    private String remarks;

    @Column("status")
    private String status;

    @Column("assessed_by")
    private String assessedBy;

    @Column("assessed_at")
    private LocalDateTime assessedAt;

    @Transient
    private boolean isNew = true;

    @Override
    public String getId() {
        return assessmentId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }
}
