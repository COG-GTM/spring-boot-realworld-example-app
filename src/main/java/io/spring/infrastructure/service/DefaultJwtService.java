package io.spring.infrastructure.service;

import io.jsonwebtoken.Jwts;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultJwtService implements JwtService {
  private final SecretKey signingKey;
  private int sessionTime;

  @Autowired
  public DefaultJwtService(
      @Value("${jwt.secret}") String secret, @Value("${jwt.sessionTime}") int sessionTime) {
    this.sessionTime = sessionTime;
    byte[] keyBytes = secret.getBytes();
    // HS512 requires a key of at least 64 bytes (512 bits)
    if (keyBytes.length < 64) {
      keyBytes = Arrays.copyOf(keyBytes, 64);
    }
    this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA512");
  }

  @Override
  public String toToken(User user) {
    return Jwts.builder()
        .subject(user.getId())
        .expiration(expireTimeFromNow())
        .signWith(signingKey)
        .compact();
  }

  @Override
  public Optional<String> getSubFromToken(String token) {
    try {
      return Optional.ofNullable(
          Jwts.parser()
              .verifyWith(signingKey)
              .build()
              .parseSignedClaims(token)
              .getPayload()
              .getSubject());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private Date expireTimeFromNow() {
    return new Date(System.currentTimeMillis() + sessionTime * 1000L);
  }
}
