package com.gs.collateral.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
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
        AbstractAuthenticationToken auth = converter.convert(jwt("risk-user", List.of("CLT_RISK_RO", "CLT_OPS")));

        assertThat(auth.getName()).isEqualTo("risk-user");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_RISK", "ROLE_OPS");
    }

    @Test
    void ignoresUnknownEntitlementsAndMissingClaim() {
        assertThat(converter.convert(jwt("x", List.of("NOT_MAPPED", "TSY_RO"))).getAuthorities())
                .extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_TREASURY");
        assertThat(converter.convert(jwt("x", null)).getAuthorities()).isEmpty();
    }

    private static GsAuthProperties properties() {
        GsAuthProperties properties = new GsAuthProperties();
        properties.setEntitlementRoles(Map.of("CLT_OPS", "OPS", "CLT_RISK_RO", "RISK", "TSY_RO", "TREASURY"));
        return properties;
    }

    private static Jwt jwt(String subject, List<String> entitlements) {
        Jwt.Builder builder = Jwt.withTokenValue("t").header("alg", "none").subject(subject)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (entitlements != null) {
            builder.claim("gs_entitlements", entitlements);
        }
        return builder.build();
    }
}
