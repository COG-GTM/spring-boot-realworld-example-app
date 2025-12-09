package com.gs.collateral.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.gs.collateral.domain.Agreement;
import com.gs.collateral.domain.MarginCall;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MarginCallNoticeFormatterTest {

    @Test
    void rendersNoticeWithFormattedAmount() {
        Agreement agreement = new Agreement("CSA-ACME-2019-01", "ACME CAPITAL LLP", "USD",
                BigDecimal.ZERO, BigDecimal.ZERO);
        MarginCall call = new MarginCall(agreement, new BigDecimal("1901000.00"), Instant.parse("2026-03-02T15:00:00Z"));

        String notice = new MarginCallNoticeFormatter().format(call);

        assertThat(notice).contains("Agreement: CSA-ACME-2019-01")
                .contains("Call amount: USD 1,901,000")
                .contains("Issued: 2026-03-02T15:00:00Z");
    }
}
