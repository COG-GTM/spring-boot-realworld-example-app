package com.gs.collateral.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/** Collateral currently held under an agreement, valued after haircut. */
@Entity
@Table(name = "collateral_position")
public class CollateralPosition {

    private static final BigDecimal BPS_IN_UNIT = BigDecimal.valueOf(10_000);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    @Column(nullable = false, length = 80)
    private String asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 10)
    private AssetClass assetClass;

    @Column(name = "market_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal marketValue;

    @Column(name = "haircut_bps", nullable = false)
    private int haircutBps;

    protected CollateralPosition() {
    }

    public CollateralPosition(String asset, AssetClass assetClass, BigDecimal marketValue, int haircutBps) {
        this.asset = asset;
        this.assetClass = assetClass;
        this.marketValue = marketValue;
        this.haircutBps = haircutBps;
    }

    /** Market value net of haircut, i.e. what the position is worth as collateral. */
    public BigDecimal collateralValue() {
        BigDecimal retained = BPS_IN_UNIT.subtract(BigDecimal.valueOf(haircutBps));
        return marketValue.multiply(retained).divide(BPS_IN_UNIT, 2, RoundingMode.DOWN);
    }

    public Long getId() {
        return id;
    }

    public Agreement getAgreement() {
        return agreement;
    }

    void setAgreement(Agreement agreement) {
        this.agreement = agreement;
    }

    public String getAsset() {
        return asset;
    }

    public AssetClass getAssetClass() {
        return assetClass;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public int getHaircutBps() {
        return haircutBps;
    }
}
