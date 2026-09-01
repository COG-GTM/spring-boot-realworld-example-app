package io.spring.infrastructure.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultJwtService implements JwtService {
  private static final Logger log = LoggerFactory.getLogger(DefaultJwtService.class);
  private static final int MINIMUM_SECRET_BYTES = 64;

  private final SecretKey signingKey;
  private final SignatureAlgorithm signatureAlgorithm;
  private int sessionTime;

  @Autowired
  public DefaultJwtService(
      @Value("${jwt.secret:}") String secret, @Value("${jwt.sessionTime}") int sessionTime) {
    this.sessionTime = sessionTime;
    this.signatureAlgorithm = SignatureAlgorithm.HS512;
    this.signingKey = buildSigningKey(secret, signatureAlgorithm);
  }

  private static SecretKey buildSigningKey(String secret, SignatureAlgorithm algorithm) {
    if (secret == null || secret.trim().isEmpty()) {
      log.warn(
          "No JWT signing secret configured (set the JWT_SECRET environment variable to at least "
              + "{} bytes). Generating a random key: tokens will be invalidated on every restart "
              + "and will not be accepted by other instances.",
          MINIMUM_SECRET_BYTES);
      return Keys.secretKeyFor(algorithm);
    }
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < MINIMUM_SECRET_BYTES) {
      throw new IllegalStateException(
          "The JWT signing secret must be at least "
              + MINIMUM_SECRET_BYTES
              + " bytes long to be used with "
              + algorithm.getValue());
    }
    return new SecretKeySpec(keyBytes, algorithm.getJcaName());
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
}
