package com.gs.collateral.api;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gs.collateral.security.DeskTokenCodec;
import com.gs.collateral.security.DeskTokenProperties;
import com.gs.collateral.security.GsEntitlementsConverter;
import com.gs.collateral.security.LegacyDeskTokenFilter;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * GS Auth path with the legacy kill-switch at its default (off). Mirrors the authorization matrix of
 * {@link MarginCallApiTest}: margin-calls need OPS, agreements need OPS/RISK/TREASURY.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GsAuthApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private GsEntitlementsConverter converter;
    @Autowired
    private ApplicationContext context;
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
    void treasuryEntitlementCannotRunMarginCycle() throws Exception {
        mockMvc.perform(post("/api/margin-calls/run").with(gsJwt("tsy-user", "TSY_RO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void riskOnlyEntitlementCannotRunMarginCycle() throws Exception {
        mockMvc.perform(post("/api/margin-calls/run").with(gsJwt("risk-user", "CLT_RISK_RO")))
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
        mockMvc.perform(get("/api/agreements/CSA-NORTHWIND-2021-07").with(gsJwt("tsy-user", "TSY_RO")))
                .andExpect(status().isOk());
    }

    @Test
    void unknownEntitlementsGrantNothing() throws Exception {
        mockMvc.perform(get("/api/agreements/CSA-NORTHWIND-2021-07").with(gsJwt("nobody", "SOMETHING_ELSE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownAgreementIs404() throws Exception {
        mockMvc.perform(get("/api/agreements/CSA-NOPE").with(gsJwt("risk-user", "CLT_RISK_RO")))
                .andExpect(status().isNotFound());
    }

    @Test
    void bearerTokenGoesThroughDecoderAndEntitlementsConverter() throws Exception {
        Jwt jwt = Jwt.withTokenValue("opaque")
                .header("alg", "RS256")
                .subject("ops-user")
                .audience(List.of("CLT-COLLATERAL-SVC"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("gs_entitlements", List.of("CLT_OPS"))
                .build();
        when(jwtDecoder.decode("opaque")).thenReturn(jwt);
        when(jwtDecoder.decode("unknown")).thenThrow(new BadJwtException("unknown issuer"));

        mockMvc.perform(get("/api/margin-calls").header("Authorization", "Bearer opaque"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/margin-calls").header("Authorization", "Bearer unknown"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void legacyDeskTokenIsIgnoredWhenKillSwitchIsOff() throws Exception {
        String token = DeskTokenCodec.issue("COLL-OPS", clock.instant().getEpochSecond(),
                deskTokenProperties.getSharedSecret());
        mockMvc.perform(get("/api/margin-calls").header("X-Desk-Token", token))
                .andExpect(status().isUnauthorized());
        org.assertj.core.api.Assertions.assertThat(context.getBeanNamesForType(LegacyDeskTokenFilter.class)).isEmpty();
    }

    private JwtRequestPostProcessor gsJwt(String subject, String... entitlements) {
        return jwt()
                .jwt(builder -> builder.subject(subject)
                        .audience(List.of("CLT-COLLATERAL-SVC"))
                        .claim("gs_entitlements", List.of(entitlements)))
                .authorities(converter::authorities);
    }
}
