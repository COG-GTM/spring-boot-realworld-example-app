package com.gs.collateral.api.dto;

import com.gs.collateral.domain.MarginCall;
import java.math.BigDecimal;
import java.time.Instant;

public class MarginCallResponse {

    private final Long id;
    private final String agreementReference;
    private final BigDecimal callAmount;
    private final String currency;
    private final MarginCall.Status status;
    private final Instant issuedAt;

    public MarginCallResponse(MarginCall call) {
        this.id = call.getId();
        this.agreementReference = call.getAgreement().getReference();
        this.callAmount = call.getCallAmount();
        this.currency = call.getCurrency();
        this.status = call.getStatus();
        this.issuedAt = call.getIssuedAt();
    }

    public Long getId() {
        return id;
    }

    public String getAgreementReference() {
        return agreementReference;
    }

    public BigDecimal getCallAmount() {
        return callAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public MarginCall.Status getStatus() {
        return status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
