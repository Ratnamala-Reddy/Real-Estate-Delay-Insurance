package org.example.paymentservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("compensation_payments")
public class CompensationPayment implements Persistable<String> {

    @Id
    @Column("payment_id")
    private String paymentId;

    @Column("claim_id")
    private String claimId;

    @Column("policy_id")
    private String policyId;

    @Column("payout_amount")
    private Double payoutAmount;

    @Column("transaction_reference")
    private String transactionReference;

    @Column("status")
    private String status;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Transient
    private boolean isNew = true;

    @Override
    public String getId() {
        return paymentId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public Double getPayoutAmount() {
        return payoutAmount;
    }

    public void setPayoutAmount(Double payoutAmount) {
        this.payoutAmount = payoutAmount;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}