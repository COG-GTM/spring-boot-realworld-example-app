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
  static final int MINIMUM_SECRET_BYTES = 64;

  private static final Set<String> REVOKED_SECRET_SHA256 =
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

  private static byte[] validatedSecret(String secret) {
    if (secret == null || secret.trim().isEmpty()) {
      throw new IllegalStateException(
          "JWT signing secret is not configured; set the JWT_SECRET environment variable");
    }
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < MINIMUM_SECRET_BYTES) {
      throw new IllegalStateException(
          "JWT signing secret must be at least " + MINIMUM_SECRET_BYTES + " bytes for HS512");
    }
    if (REVOKED_SECRET_SHA256.contains(sha256Hex(bytes))) {
      throw new IllegalStateException(
          "JWT signing secret was previously committed to source control and must be rotated");
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

  @Override
  public String toToken(User user) {
    return Jwts.builder()
        .setSubject(user.getId())
        .setExpiration(expireTimeFromNow())
        .signWith(signingKey)
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
}
