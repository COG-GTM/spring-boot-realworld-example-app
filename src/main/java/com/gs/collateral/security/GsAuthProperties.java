package com.gs.collateral.security;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GS Auth: bearer JWTs issued by GS SSO (OIDC), validated as an OAuth2 resource server. Roles are
 * derived from the entitlements claim through {@link #entitlementRoles}.
 */
@ConfigurationProperties(prefix = "gs.auth")
public class GsAuthProperties {

    private String issuerUri;
    private String audience;
    private String entitlementsClaim = "gs_entitlements";
    /** entitlement -> role (without the {@code ROLE_} prefix) */
    private Map<String, String> entitlementRoles = new LinkedHashMap<>();

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getEntitlementsClaim() {
        return entitlementsClaim;
    }

    public void setEntitlementsClaim(String entitlementsClaim) {
        this.entitlementsClaim = entitlementsClaim;
    }

    public Map<String, String> getEntitlementRoles() {
        return entitlementRoles;
    }

    public void setEntitlementRoles(Map<String, String> entitlementRoles) {
        this.entitlementRoles = entitlementRoles;
    }
}
