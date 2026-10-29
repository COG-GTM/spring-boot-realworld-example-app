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
    void mapsEntitlementsToRolesAndUsesSubjectAsPrincipal() {
        AbstractAuthenticationToken auth = converter.convert(jwt("ops-user", List.of("CLT_OPS", "CLT_RISK_RO")));

        assertThat(auth.getName()).isEqualTo("ops-user");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_OPS", "ROLE_RISK");
    }

    @Test
    void ignoresUnknownEntitlementsAndMissingClaim() {
        assertThat(converter.convert(jwt("u", List.of("EQ_TRADER", "TSY_RO"))).getAuthorities())
                .extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_TREASURY");
        assertThat(converter.convert(Jwt.withTokenValue("t").header("alg", "none").subject("u").build())
                .getAuthorities()).isEmpty();
    }

    private static GsAuthProperties properties() {
        GsAuthProperties properties = new GsAuthProperties();
        properties.setEntitlementRoles(Map.of("CLT_OPS", "OPS", "CLT_RISK_RO", "RISK", "TSY_RO", "TREASURY"));
        return properties;
    }

    private static Jwt jwt(String subject, List<String> entitlements) {
        return Jwt.withTokenValue("t").header("alg", "none").subject(subject)
                .claim("gs_entitlements", entitlements).build();
    }
}
