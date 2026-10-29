package com.gs.collateral.service;

import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.format.annotation.DateTimeFormat;

@ConfigurationProperties(prefix = "collateral.margin")
public class MarginProperties {

    private final long roundingUnit;
    private final LocalTime callCutoff;
    private final ZoneId timezone;

    public MarginProperties(long roundingUnit,
                            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime callCutoff,
                            ZoneId timezone) {
        this.roundingUnit = roundingUnit;
        this.callCutoff = callCutoff;
        this.timezone = timezone;
    }

    public long getRoundingUnit() {
        return roundingUnit;
    }

    public LocalTime getCallCutoff() {
        return callCutoff;
    }

    public ZoneId getTimezone() {
        return timezone;
    }
}
