package com.gs.collateral.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.gs.collateral.domain.AssetClass;
import org.junit.jupiter.api.Test;

class EligibilityServiceTest {

    private final EligibilityService service = new EligibilityService();

    @Test
    void investmentGradeCorporatesAreEligibleAtEightPercentHaircut() {
        assertThat(service.isEligible(AssetClass.CORP, 800)).isTrue();
        assertThat(service.isEligible(AssetClass.CORP, 500)).isFalse();
    }

    @Test
    void equityRemainsIneligible() {
        assertThat(service.isEligible(AssetClass.EQUITY, 5000)).isFalse();
    }
}
