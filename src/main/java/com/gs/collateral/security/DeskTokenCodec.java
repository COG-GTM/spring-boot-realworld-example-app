package com.gs.collateral.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Token format: {@code <desk>:<epochSeconds>:<hex hmac-sha256 of "desk:epochSeconds">}. */
public final class DeskTokenCodec {

    private static final String ALGORITHM = "HmacSHA256";

    private DeskTokenCodec() {
    }

    public static String issue(String desk, long epochSeconds, String secret) {
        return desk + ":" + epochSeconds + ":" + sign(desk + ":" + epochSeconds, secret);
    }

    public static Optional<ParsedToken> verify(String token, String secret, long nowEpochSeconds, long maxSkew) {
        if (token == null) {
            return Optional.empty();
        }
        String[] parts = token.split(":");
        if (parts.length != 3) {
            return Optional.empty();
        }
        long issued;
        try {
            issued = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        if (Math.abs(nowEpochSeconds - issued) > maxSkew) {
            return Optional.empty();
        }
        byte[] expected = sign(parts[0] + ":" + parts[1], secret).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = parts[2].getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, actual)) {
            return Optional.empty();
        }
        return Optional.of(new ParsedToken(parts[0], issued));
    }

    static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC unavailable", e);
        }
    }

    public static final class ParsedToken {
        private final String desk;
        private final long issuedEpochSeconds;

        ParsedToken(String desk, long issuedEpochSeconds) {
            this.desk = desk;
            this.issuedEpochSeconds = issuedEpochSeconds;
        }

        public String getDesk() {
            return desk;
        }

        public long getIssuedEpochSeconds() {
            return issuedEpochSeconds;
        }
    }
}
