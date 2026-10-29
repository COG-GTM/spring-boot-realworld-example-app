package com.gs.collateral.api;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gs.collateral.security.DeskTokenCodec;
import com.gs.collateral.security.DeskTokenProperties;
import com.gs.collateral.security.GsEntitlementsConverter;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * GS Auth authorization matrix: must match the legacy desk-token matrix in {@link MarginCallApiTest}.
 * Runs with the kill-switch at its default (off), so desk tokens are no longer honoured.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MarginCallGsAuthApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private GsEntitlementsConverter converter;
    @Autowired
    private DeskTokenProperties deskTokenProperties;
    @Autowired
    private Clock clock;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void rejectsAnonymousCallers() throws Exception {
        mockMvc.perform(get("/api/margin-calls")).andExpect(status().isUnauthorized());
    }

    @Test
    void legacyDeskTokenIsIgnoredWhenKillSwitchOff() throws Exception {
        String token = DeskTokenCodec.issue("COLL-OPS", clock.instant().getEpochSecond(),
                deskTokenProperties.getSharedSecret());
        mockMvc.perform(get("/api/margin-calls").header("X-Desk-Token", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void treasuryEntitlementCannotRunMarginCycle() throws Exception {
        mockMvc.perform(post("/api/margin-calls/run").with(gsJwt("tsy-user", "TSY_RO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void riskOnlyEntitlementCannotTouchMarginCalls() throws Exception {
        mockMvc.perform(get("/api/margin-calls").with(gsJwt("risk-user", "CLT_RISK_RO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownEntitlementsGrantNothing() throws Exception {
        mockMvc.perform(get("/api/agreements").with(gsJwt("someone", "FX_TRADER", "CLT_ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void opsEntitlementRunsMarginCycleAgainstSeedData() throws Exception {
        mockMvc.perform(post("/api/margin-calls/run").with(gsJwt("ops-user", "CLT_OPS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].agreementReference").value("CSA-ACME-2019-01"))
                .andExpect(jsonPath("$[0].callAmount").value(820000.00))
                .andExpect(jsonPath("$[0].status").value("ISSUED"));

        mockMvc.perform(get("/api/margin-calls/1/notice").with(gsJwt("risk-ops-user", "CLT_RISK_RO", "CLT_OPS")))
                .andExpect(status().isOk());
    }

    @Test
    void riskEntitlementCanReadAgreements() throws Exception {
        mockMvc.perform(get("/api/agreements/CSA-NORTHWIND-2021-07").with(gsJwt("risk-user", "CLT_RISK_RO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collateralHeld").value(2955000.00));
    }

    @Test
    void treasuryEntitlementCanReadAgreements() throws Exception {
        mockMvc.perform(get("/api/agreements").with(gsJwt("tsy-user", "TSY_RO")))
                .andExpect(status().isOk());
    }

    @Test
    void unknownAgreementIs404() throws Exception {
        mockMvc.perform(get("/api/agreements/CSA-NOPE").with(gsJwt("risk-user", "CLT_RISK_RO")))
                .andExpect(status().isNotFound());
    }

    private JwtRequestPostProcessor gsJwt(String subject, String... entitlements) {
        return jwt()
                .jwt(jwt -> jwt.subject(subject)
                        .audience(List.of("CLT-COLLATERAL-SVC"))
                        .claim("gs_entitlements", List.of(entitlements)))
                .authorities(converter::authorities);
    }
}
