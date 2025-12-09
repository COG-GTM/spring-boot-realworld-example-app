package com.gs.collateral.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.gs.collateral.domain.Agreement;
import com.gs.collateral.domain.AssetClass;
import com.gs.collateral.domain.CollateralPosition;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ExposureServiceTest {

    private final ExposureService service = new ExposureService();

    @Test
    void appliesHaircutsBeforeSummingCollateral() {
        Agreement agreement = agreement("12500000");
        agreement.addPosition(new CollateralPosition("UST 2.5% 2031", AssetClass.GOVT, new BigDecimal("12500000"), 200));
        agreement.addPosition(new CollateralPosition("IBM 3.3% 2027", AssetClass.CORP, new BigDecimal("4000000"), 800));

        assertThat(service.collateralHeld(agreement)).isEqualByComparingTo("15930000.00");
        assertThat(service.uncollateralisedExposure(agreement)).isEqualByComparingTo("-3430000.00");
    }

    @Test
    void noCollateralMeansFullExposureIsUncollateralised() {
        Agreement agreement = agreement("80000");
        assertThat(service.uncollateralisedExposure(agreement)).isEqualByComparingTo("80000");
    }

    private static Agreement agreement(String exposure) {
        Agreement agreement = new Agreement("CSA-TEST-1", "TEST CP", "USD", BigDecimal.ZERO, BigDecimal.ZERO);
        agreement.setExposure(new BigDecimal(exposure));
        return agreement;
    }
}
