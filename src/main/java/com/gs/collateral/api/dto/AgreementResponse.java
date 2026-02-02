package com.gs.collateral.api.dto;

import com.gs.collateral.domain.Agreement;
import java.math.BigDecimal;

public class AgreementResponse {

    private final String reference;
    private final String counterparty;
    private final String currency;
    private final BigDecimal threshold;
    private final BigDecimal minimumTransferAmount;
    private final BigDecimal exposure;
    private final BigDecimal collateralHeld;
    private final int positionCount;

    public AgreementResponse(Agreement agreement, BigDecimal collateralHeld) {
        this.reference = agreement.getReference();
        this.counterparty = agreement.getCounterparty();
        this.currency = agreement.getCurrency();
        this.threshold = agreement.getThreshold();
        this.minimumTransferAmount = agreement.getMinimumTransferAmount();
        this.exposure = agreement.getExposure();
        this.collateralHeld = collateralHeld;
        this.positionCount = agreement.getPositions().size();
    }

    public String getReference() {
        return reference;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public BigDecimal getMinimumTransferAmount() {
        return minimumTransferAmount;
    }

    public BigDecimal getExposure() {
        return exposure;
    }

    public BigDecimal getCollateralHeld() {
        return collateralHeld;
    }

    public int getPositionCount() {
        return positionCount;
    }
}
