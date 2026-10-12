package org.example.verifyservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("verification_records")
public class VerificationRecord implements Persistable<String> {

    @Id
    @Column("verification_id")
    private String verificationId;

    @Column("application_id")
    private String applicationId;

    @Column("verification_status")
    private String verificationStatus;

    @Column("remarks")
    private String remarks;

    @Column("verified_by")
    private String verifiedBy;

    @Column("verified_at")
    private LocalDateTime verifiedAt;

    @Transient
    private boolean isNew = true;

    @Override
    public String getId() {
        return verificationId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }
}