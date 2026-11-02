package com.gs.collateral.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Maps the {@code gs_entitlements} claim of a GS SSO JWT to {@code ROLE_<role>} authorities using
 * {@code gs.auth.entitlement-roles}. Unknown entitlements are ignored; the principal name is {@code sub}.
 */
@Component
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
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        List<String> entitlements = jwt.getClaimAsStringList(properties.getEntitlementsClaim());
        if (entitlements == null) {
            return authorities;
        }
        for (String entitlement : entitlements) {
            String role = properties.getEntitlementRoles().get(entitlement);
            if (role != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.trim()));
            }
        }
        return authorities;
    }
}
