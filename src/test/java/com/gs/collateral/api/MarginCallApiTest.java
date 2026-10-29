package com.gs.collateral.api;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gs.collateral.security.DeskTokenCodec;
import com.gs.collateral.security.DeskTokenProperties;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Legacy desk-token authorization matrix, kept until the filter is removed. Runs with the
 * kill-switch on; the GS Auth equivalent is {@link MarginCallGsAuthApiTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "collateral.auth.legacy-desk-token.enabled=true")
class MarginCallApiTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @Autowired
    private DeskTokenProperties deskTokenProperties;
    @Autowired
    private Clock clock;

    @Test
    void rejectsAnonymousCallers() throws Exception {
        mockMvc.perform(get("/api/margin-calls")).andExpect(status().isUnauthorized());
    }

    @Test
    void treasuryDeskCannotRunMarginCycle() throws Exception {
        mockMvc.perform(post("/api/margin-calls/run").header("X-Desk-Token", token("TREASURY")))
                .andExpect(status().isForbidden());
    }

    @Test
    void opsDeskRunsMarginCycleAgainstSeedData() throws Exception {
        mockMvc.perform(post("/api/margin-calls/run").header("X-Desk-Token", token("COLL-OPS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].agreementReference").value("CSA-ACME-2019-01"))
                .andExpect(jsonPath("$[0].callAmount").value(820000.00))
                .andExpect(jsonPath("$[0].status").value("ISSUED"));

        mockMvc.perform(get("/api/margin-calls/1/notice").header("X-Desk-Token", token("RISK")))
                .andExpect(status().isOk());
    }

    @Test
    void opsDeskExportsCsv() throws Exception {
        mockMvc.perform(post("/api/margin-calls/run").header("X-Desk-Token", token("COLL-OPS")));
        mockMvc.perform(get("/api/margin-calls/export").header("X-Desk-Token", token("COLL-OPS")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CSA-ACME-2019-01")));
    }

    @Test
    void riskDeskCanReadAgreements() throws Exception {
        mockMvc.perform(get("/api/agreements/CSA-NORTHWIND-2021-07").header("X-Desk-Token", token("RISK")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collateralHeld").value(2955000.00));
    }

    @Test
    void unknownAgreementIs404() throws Exception {
        mockMvc.perform(get("/api/agreements/CSA-NOPE").header("X-Desk-Token", token("RISK")))
                .andExpect(status().isNotFound());
    }

    private String token(String desk) {
        return DeskTokenCodec.issue(desk, clock.instant().getEpochSecond(), deskTokenProperties.getSharedSecret());
    }
}
