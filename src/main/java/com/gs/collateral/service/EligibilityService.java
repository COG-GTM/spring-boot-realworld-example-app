package com.gs.collateral.service;

import com.gs.collateral.domain.AssetClass;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Baseline collateral eligibility schedule. Counterparty-specific schedules are out of scope for now. */
@Service
public class EligibilityService {

    private static final Set<AssetClass> ELIGIBLE = EnumSet.of(AssetClass.CASH, AssetClass.GOVT, AssetClass.AGENCY);

    public boolean isEligible(AssetClass assetClass, int haircutBps) {
        return ELIGIBLE.contains(assetClass) && haircutBps >= minimumHaircutBps(assetClass);
    }

    public int minimumHaircutBps(AssetClass assetClass) {
        switch (assetClass) {
            case CASH:
                return 0;
            case GOVT:
                return 100;
            case AGENCY:
                return 300;
            default:
                return 10_000;
        }
    }
}
