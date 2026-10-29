package com.gs.collateral.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Maps each entitlement in the GS SSO entitlements claim to {@code ROLE_<role>} via
 * {@link GsAuthProperties#getEntitlementRoles()}. Unknown entitlements are ignored; the principal
 * name is the token {@code sub}.
 */
public class GsEntitlementsConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final GsAuthProperties properties;

    public GsEntitlementsConverter(GsAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, authorities(jwt), jwt.getSubject());
    }

    public Collection<GrantedAuthority> authorities(Jwt jwt) {
        List<String> entitlements = jwt.getClaimAsStringList(properties.getEntitlementsClaim());
        if (entitlements == null) {
            return List.of();
        }
        Map<String, String> roles = properties.getEntitlementRoles();
        return entitlements.stream()
                .map(roles::get)
                .filter(Objects::nonNull)
                .distinct()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
}
