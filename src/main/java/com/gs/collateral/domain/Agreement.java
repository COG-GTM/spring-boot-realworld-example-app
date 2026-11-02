package com.gs.collateral.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/** A Credit Support Annex (CSA) agreement with a counterparty. */
@Entity
@Table(name = "csa_agreement")
public class Agreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String reference;

    @Column(nullable = false, length = 120)
    private String counterparty;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal threshold;

    @Column(name = "min_transfer_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal minimumTransferAmount;

    /** Mark-to-market exposure to the counterparty, positive means they owe us. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal exposure = BigDecimal.ZERO;

    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CollateralPosition> positions = new ArrayList<>();

    protected Agreement() {
    }

    public Agreement(String reference, String counterparty, String currency,
                     BigDecimal threshold, BigDecimal minimumTransferAmount) {
        this.reference = reference;
        this.counterparty = counterparty;
        this.currency = currency;
        this.threshold = threshold;
        this.minimumTransferAmount = minimumTransferAmount;
    }

    public void addPosition(CollateralPosition position) {
        position.setAgreement(this);
        positions.add(position);
    }

    public Long getId() {
        return id;
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

    public void setExposure(BigDecimal exposure) {
        this.exposure = exposure;
    }

    public List<CollateralPosition> getPositions() {
        return positions;
    }
}
