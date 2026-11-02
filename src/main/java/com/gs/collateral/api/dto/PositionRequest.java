package com.gs.collateral.api.dto;

import com.gs.collateral.domain.AssetClass;
import java.math.BigDecimal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class PositionRequest {

    @NotBlank
    private String asset;
    @NotNull
    private AssetClass assetClass;
    @NotNull
    @PositiveOrZero
    private BigDecimal marketValue;
    @Min(0)
    @Max(10_000)
    private int haircutBps;

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public AssetClass getAssetClass() {
        return assetClass;
    }

    public void setAssetClass(AssetClass assetClass) {
        this.assetClass = assetClass;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = marketValue;
    }

    public int getHaircutBps() {
        return haircutBps;
    }

    public void setHaircutBps(int haircutBps) {
        this.haircutBps = haircutBps;
    }
}
