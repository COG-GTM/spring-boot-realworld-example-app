package com.gs.collateral.service;

import com.gs.collateral.domain.Agreement;
import com.gs.collateral.domain.MarginCall;
import com.gs.collateral.repository.AgreementRepository;
import com.gs.collateral.repository.MarginCallRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MarginCallService {

    private static final Logger log = LoggerFactory.getLogger(MarginCallService.class);

    private final AgreementRepository agreements;
    private final MarginCallRepository marginCalls;
    private final ExposureService exposureService;
    private final MarginProperties properties;
    private final Clock clock;

    public MarginCallService(AgreementRepository agreements, MarginCallRepository marginCalls,
                             ExposureService exposureService, MarginProperties properties, Clock clock) {
        this.agreements = agreements;
        this.marginCalls = marginCalls;
        this.exposureService = exposureService;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Issues a margin call when the uncollateralised exposure exceeds the CSA threshold
     * by at least the minimum transfer amount. Amounts are rounded up to the rounding unit.
     */
    @Transactional
    public Optional<MarginCall> evaluate(Agreement agreement) {
        BigDecimal shortfall = exposureService.uncollateralisedExposure(agreement)
                .subtract(agreement.getThreshold());
        if (shortfall.compareTo(agreement.getMinimumTransferAmount()) < 0) {
            log.debug("{}: shortfall {} below MTA {}, no call", agreement.getReference(),
                    shortfall, agreement.getMinimumTransferAmount());
            return Optional.empty();
        }
        BigDecimal amount = roundUp(shortfall, properties.getRoundingUnit());
        MarginCall call = marginCalls.save(new MarginCall(agreement, amount, clock.instant()));
        log.info("{}: issued margin call {} {} (id {})", agreement.getReference(), amount,
                agreement.getCurrency(), call.getId());
        return Optional.of(call);
    }

    @Transactional
    public List<MarginCall> runDailyCycle() {
        List<MarginCall> issued = new ArrayList<>();
        for (Agreement agreement : agreements.findAll()) {
            evaluate(agreement).ifPresent(issued::add);
        }
        return issued;
    }

    @Transactional
    public MarginCall agree(long id) {
        MarginCall call = marginCalls.findById(id)
                .orElseThrow(() -> new NotFoundException("Margin call " + id + " not found"));
        call.agree();
        return call;
    }

    @Transactional
    public MarginCall dispute(long id, String reason) {
        MarginCall call = marginCalls.findById(id)
                .orElseThrow(() -> new NotFoundException("Margin call " + id + " not found"));
        call.dispute(reason);
        log.info("{}: margin call {} disputed: {}", call.getAgreement().getReference(), id, reason);
        return call;
    }

    static BigDecimal roundUp(BigDecimal amount, long unit) {
        BigDecimal step = BigDecimal.valueOf(unit);
        return amount.divide(step, 0, RoundingMode.CEILING).multiply(step).setScale(2, RoundingMode.UNNECESSARY);
    }
}
