package com.gs.collateral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gs.collateral.domain.Agreement;
import com.gs.collateral.domain.MarginCall;
import com.gs.collateral.repository.AgreementRepository;
import com.gs.collateral.repository.MarginCallRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarginCallServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-02T15:00:00Z");

    @Mock
    private AgreementRepository agreements;
    @Mock
    private MarginCallRepository marginCalls;

    private MarginCallService service;

    @BeforeEach
    void setUp() {
        MarginProperties props = new MarginProperties(1000, LocalTime.of(16, 30), ZoneId.of("America/New_York"));
        service = new MarginCallService(agreements, marginCalls, new ExposureService(), props,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void issuesRoundedCallWhenShortfallExceedsMinimumTransferAmount() {
        Agreement agreement = new Agreement("CSA-A", "CP", "USD", new BigDecimal("1000000"), new BigDecimal("250000"));
        agreement.setExposure(new BigDecimal("2900500.00"));
        when(marginCalls.save(any(MarginCall.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<MarginCall> call = service.evaluate(agreement);

        assertThat(call).isPresent();
        assertThat(call.get().getCallAmount()).isEqualByComparingTo("1901000.00");
        assertThat(call.get().getCurrency()).isEqualTo("USD");
        assertThat(call.get().getIssuedAt()).isEqualTo(NOW);
    }

    @Test
    void doesNotCallBelowMinimumTransferAmount() {
        Agreement agreement = new Agreement("CSA-B", "CP", "USD", new BigDecimal("1000000"), new BigDecimal("250000"));
        agreement.setExposure(new BigDecimal("1200000"));

        assertThat(service.evaluate(agreement)).isEmpty();
        verify(marginCalls, never()).save(any());
    }

    @Test
    void roundsUpToUnit() {
        assertThat(MarginCallService.roundUp(new BigDecimal("1000.01"), 1000)).isEqualByComparingTo("2000");
        assertThat(MarginCallService.roundUp(new BigDecimal("3000"), 1000)).isEqualByComparingTo("3000");
    }
}
