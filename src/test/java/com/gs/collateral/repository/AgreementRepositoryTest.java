package com.gs.collateral.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gs.collateral.domain.Agreement;
import com.gs.collateral.domain.AssetClass;
import com.gs.collateral.domain.CollateralPosition;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class AgreementRepositoryTest {

    @Autowired
    private AgreementRepository repository;

    @Test
    void persistsAgreementWithPositions() {
        Agreement agreement = new Agreement("CSA-REPO-1", "REPO CP", "GBP", new BigDecimal("100.00"), new BigDecimal("10.00"));
        agreement.addPosition(new CollateralPosition("GILT 2035", AssetClass.GOVT, new BigDecimal("500.00"), 100));
        repository.saveAndFlush(agreement);

        assertThat(repository.findByReference("CSA-REPO-1"))
                .hasValueSatisfying(a -> assertThat(a.getPositions()).hasSize(1));
        assertThat(repository.findByReference("CSA-MISSING")).isEmpty();
    }
}
