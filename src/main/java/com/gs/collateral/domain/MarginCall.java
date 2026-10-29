package com.gs.collateral.domain;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "margin_call")
public class MarginCall {

    public enum Status { ISSUED, AGREED, DISPUTED, SETTLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    @Column(name = "dispute_reason", length = 500)
    private String disputeReason;

    @Column(name = "call_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal callAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.ISSUED;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    protected MarginCall() {
    }

    public MarginCall(Agreement agreement, BigDecimal callAmount, Instant issuedAt) {
        this.agreement = agreement;
        this.callAmount = callAmount;
        this.currency = agreement.getCurrency();
        this.issuedAt = issuedAt;
    }

    public void agree() {
        if (status != Status.ISSUED) {
            throw new IllegalStateException("Margin call " + id + " is " + status + ", cannot be agreed");
        }
        status = Status.AGREED;
    }

    public void dispute(String reason) {
        if (status != Status.ISSUED) {
            throw new IllegalStateException("Margin call " + id + " is " + status + ", cannot be disputed");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Dispute reason is required");
        }
        status = Status.DISPUTED;
        disputeReason = reason;
    }

    public String getDisputeReason() {
        return disputeReason;
    }

    public Long getId() {
        return id;
    }

    public Agreement getAgreement() {
        return agreement;
    }

    public BigDecimal getCallAmount() {
        return callAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
