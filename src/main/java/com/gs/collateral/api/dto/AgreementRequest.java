package com.gs.collateral.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public class AgreementRequest {

    @NotBlank
    @Pattern(regexp = "CSA-[A-Z0-9-]{3,30}")
    private String reference;
    @NotBlank
    private String counterparty;
    @NotBlank
    @Pattern(regexp = "[A-Z]{3}")
    private String currency;
    @NotNull
    @PositiveOrZero
    private BigDecimal threshold;
    @NotNull
    @PositiveOrZero
    private BigDecimal minimumTransferAmount;
    @NotNull
    private BigDecimal exposure;
    @Valid
    private List<PositionRequest> positions = new ArrayList<>();

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public void setThreshold(BigDecimal threshold) {
        this.threshold = threshold;
    }

    public BigDecimal getMinimumTransferAmount() {
        return minimumTransferAmount;
    }

    public void setMinimumTransferAmount(BigDecimal minimumTransferAmount) {
        this.minimumTransferAmount = minimumTransferAmount;
    }

    public BigDecimal getExposure() {
        return exposure;
    }

    public void setExposure(BigDecimal exposure) {
        this.exposure = exposure;
    }

    public List<PositionRequest> getPositions() {
        return positions;
    }

    public void setPositions(List<PositionRequest> positions) {
        this.positions = positions;
    }
}
