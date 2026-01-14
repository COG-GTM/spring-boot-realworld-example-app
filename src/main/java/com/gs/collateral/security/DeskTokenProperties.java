package com.gs.collateral.security;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Legacy desk-token authentication (pre-GS-auth). Each trading desk shares a secret
 * with this service and presents an HMAC-signed header on every request.
 */
@ConfigurationProperties(prefix = "collateral.auth.desk-token")
public class DeskTokenProperties {

    private String header = "X-Desk-Token";
    private String sharedSecret;
    private long maxSkewSeconds = 300;
    /** desk id -> comma separated roles */
    private Map<String, String> desks = new LinkedHashMap<>();

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public long getMaxSkewSeconds() {
        return maxSkewSeconds;
    }

    public void setMaxSkewSeconds(long maxSkewSeconds) {
        this.maxSkewSeconds = maxSkewSeconds;
    }

    public Map<String, String> getDesks() {
        return desks;
    }

    public void setDesks(Map<String, String> desks) {
        this.desks = desks;
    }
}
