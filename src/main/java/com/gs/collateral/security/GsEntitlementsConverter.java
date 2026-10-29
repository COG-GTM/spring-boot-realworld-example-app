package com.gs.collateral.security;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Maps the {@code gs_entitlements} claim of a GS SSO token to {@code ROLE_<role>} authorities using
 * {@link GsAuthProperties#getEntitlementRoles()}. Entitlements without a mapping are ignored; the
 * principal name is the token subject.
 */
public class GsEntitlementsConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger log = LoggerFactory.getLogger(GsEntitlementsConverter.class);

    private final GsAuthProperties properties;

    public GsEntitlementsConverter(GsAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<String> entitlements = jwt.getClaimAsStringList(properties.getEntitlementsClaim());
        List<GrantedAuthority> authorities = entitlements == null ? List.of() : entitlements.stream()
                .map(this::toRole)
                .filter(Objects::nonNull)
                .distinct()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private String toRole(String entitlement) {
        String role = properties.getEntitlementRoles().get(entitlement);
        if (role == null) {
            log.debug("Ignoring unmapped entitlement '{}'", entitlement);
        }
        return role;
    }
}
