package com.gs.collateral.api;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gs.collateral.security.GsAuthProperties;
import com.gs.collateral.security.GsEntitlementsConverter;
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
 * GS Auth authorization matrix. Mirrors {@link MarginCallApiTest} (legacy desk tokens) so both
 * paths are proven to enforce the same rules: COLL-OPS -> CLT_OPS, RISK -> CLT_RISK_RO,
 * TREASURY -> TSY_RO.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GsAuthApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private GsAuthProperties gsAuthProperties;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void rejectsAnonymousCallers() throws Exception {
        mockMvc.perform(get("/api/margin-calls")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsLegacyDeskTokensWhenKillSwitchIsOff() throws Exception {
        mockMvc.perform(get("/api/agreements/CSA-NORTHWIND-2021-07").header("X-Desk-Token", "RISK:0:deadbeef"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void treasuryEntitlementCannotRunMarginCycle() throws Exception {
        mockMvc.perform(post("/api/margin-calls/run").with(gsToken("tsy-user", "TSY_RO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unmappedEntitlementsGrantNothing() throws Exception {
        mockMvc.perform(get("/api/agreements/CSA-NORTHWIND-2021-07").with(gsToken("someone", "EQ_TRADER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void opsEntitlementRunsMarginCycleAgainstSeedData() throws Exception {
        mockMvc.perform(post("/api/margin-calls/run").with(gsToken("ops-user", "CLT_OPS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].agreementReference").value("CSA-ACME-2019-01"))
                .andExpect(jsonPath("$[0].callAmount").value(820000.00))
                .andExpect(jsonPath("$[0].status").value("ISSUED"));

        mockMvc.perform(get("/api/margin-calls/1/notice").with(gsToken("ops-user", "CLT_OPS")))
                .andExpect(status().isOk());
    }

    @Test
    void riskEntitlementCanReadAgreementsButNotMarginCalls() throws Exception {
        mockMvc.perform(get("/api/agreements/CSA-NORTHWIND-2021-07").with(gsToken("risk-user", "CLT_RISK_RO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collateralHeld").value(2955000.00));
        mockMvc.perform(get("/api/margin-calls").with(gsToken("risk-user", "CLT_RISK_RO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownAgreementIs404() throws Exception {
        mockMvc.perform(get("/api/agreements/CSA-NOPE").with(gsToken("risk-user", "CLT_RISK_RO")))
                .andExpect(status().isNotFound());
    }

    private JwtRequestPostProcessor gsToken(String subject, String... entitlements) {
        return jwt()
                .jwt(jwt -> jwt.subject(subject)
                        .issuer(gsAuthProperties.getIssuerUri())
                        .audience(List.of(gsAuthProperties.getAudience()))
                        .claim(gsAuthProperties.getEntitlementsClaim(), List.of(entitlements)))
                .authorities(jwt -> new GsEntitlementsConverter(gsAuthProperties).convert(jwt).getAuthorities());
    }
}
