package com.gs.collateral.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeskTokenCodecTest {

    private static final String SECRET = "unit-test-secret";

    @Test
    void roundTripsValidToken() {
        String token = DeskTokenCodec.issue("RISK", 1_700_000_000L, SECRET);
        assertThat(DeskTokenCodec.verify(token, SECRET, 1_700_000_010L, 300))
                .hasValueSatisfying(t -> assertThat(t.getDesk()).isEqualTo("RISK"));
    }

    @Test
    void rejectsTamperedAndStaleTokens() {
        String token = DeskTokenCodec.issue("RISK", 1_700_000_000L, SECRET);
        assertThat(DeskTokenCodec.verify(token.replace("RISK", "COLL-OPS"), SECRET, 1_700_000_000L, 300)).isEmpty();
        assertThat(DeskTokenCodec.verify(token, "other", 1_700_000_000L, 300)).isEmpty();
        assertThat(DeskTokenCodec.verify(token, SECRET, 1_700_001_000L, 300)).isEmpty();
        assertThat(DeskTokenCodec.verify("garbage", SECRET, 1_700_000_000L, 300)).isEmpty();
    }
}
