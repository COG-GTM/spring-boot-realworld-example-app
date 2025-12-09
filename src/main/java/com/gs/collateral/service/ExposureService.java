package com.gs.collateral.service;

import com.gs.collateral.domain.Agreement;
import com.gs.collateral.domain.CollateralPosition;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class ExposureService {

    /** Sum of haircut-adjusted collateral held under the agreement. */
    public BigDecimal collateralHeld(Agreement agreement) {
        return agreement.getPositions().stream()
                .map(CollateralPosition::collateralValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Exposure not covered by collateral. Negative means we hold excess collateral. */
    public BigDecimal uncollateralisedExposure(Agreement agreement) {
        return agreement.getExposure().subtract(collateralHeld(agreement));
    }
}
