package com.gs.collateral.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class GsEntitlementsConverterTest {

    private final GsEntitlementsConverter converter = new GsEntitlementsConverter(properties());

    @Test
    void mapsKnownEntitlementsToRolesAndUsesSubAsPrincipal() {
        AbstractAuthenticationToken auth = converter.convert(
                jwt("jdoe", List.of("CLT_OPS", "TSY_RO", "FX_TRADER")));

        assertThat(auth.getName()).isEqualTo("jdoe");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_OPS", "ROLE_TREASURY");
    }

    @Test
    void missingClaimGrantsNothing() {
        AbstractAuthenticationToken auth = converter.convert(
                Jwt.withTokenValue("t").header("alg", "none").subject("jdoe").build());

        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void duplicateEntitlementsCollapseToOneRole() {
        assertThat(converter.authorities(jwt("jdoe", List.of("CLT_RISK_RO", "CLT_RISK_RO"))))
                .extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_RISK");
    }

    private static Jwt jwt(String subject, List<String> entitlements) {
        return Jwt.withTokenValue("t").header("alg", "none").subject(subject)
                .claim("gs_entitlements", entitlements).build();
    }

    private static GsAuthProperties properties() {
        GsAuthProperties p = new GsAuthProperties();
        p.setEntitlementRoles(Map.of("CLT_OPS", "OPS", "CLT_RISK_RO", "RISK", "TSY_RO", "TREASURY"));
        return p;
    }
}
