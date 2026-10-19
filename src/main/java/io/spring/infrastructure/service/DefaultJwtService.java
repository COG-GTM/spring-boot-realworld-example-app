package io.spring.infrastructure.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultJwtService implements JwtService {
  static final int MIN_SECRET_BYTES = 64;
  static final int MIN_DISTINCT_CHARS = 10;

  // SHA-256 of jwt.secret values that were committed to source control and must never be reused.
  private static final Set<String> REVOKED_SECRET_FINGERPRINTS =
      Set.of("990b0164cf2b6d7441bf1e7555d93004a915c9b3e9e57cefee56b45cd2291a41");

  private final SecretKey signingKey;
  private final SignatureAlgorithm signatureAlgorithm;
  private int sessionTime;

  @Autowired
  public DefaultJwtService(
      @Value("${jwt.secret}") String secret, @Value("${jwt.sessionTime}") int sessionTime) {
    this.sessionTime = sessionTime;
    signatureAlgorithm = SignatureAlgorithm.HS512;
    this.signingKey = new SecretKeySpec(validatedSecret(secret), signatureAlgorithm.getJcaName());
  }

  @Override
  public String toToken(User user) {
    return Jwts.builder()
        .setSubject(user.getId())
        .setExpiration(expireTimeFromNow())
        .signWith(signingKey, signatureAlgorithm)
        .compact();
  }

  @Override
  public Optional<String> getSubFromToken(String token) {
    try {
      Jws<Claims> claimsJws =
          Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);
      return Optional.ofNullable(claimsJws.getBody().getSubject());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private Date expireTimeFromNow() {
    return new Date(System.currentTimeMillis() + sessionTime * 1000L);
  }

  static byte[] validatedSecret(String secret) {
    if (secret == null || secret.trim().isEmpty() || secret.startsWith("${")) {
      throw new IllegalStateException(
          "jwt.secret is not set; provide it through the JWT_SECRET environment variable");
    }
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes (512 bits) for HS512");
    }
    if (secret.chars().distinct().count() < MIN_DISTINCT_CHARS) {
      throw new IllegalStateException(
          "jwt.secret is too predictable; use at least "
              + MIN_DISTINCT_CHARS
              + " distinct characters, e.g. a random base64 value");
    }
    if (REVOKED_SECRET_FINGERPRINTS.contains(sha256Hex(bytes))) {
      throw new IllegalStateException(
          "jwt.secret is a revoked value that was committed to source control; generate a new"
              + " one");
    }
    return bytes;
  }

  private static String sha256Hex(byte[] bytes) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
      StringBuilder hex = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
