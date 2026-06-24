package io.spring.infrastructure.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.MacAlgorithm;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
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
  private final MacAlgorithm algorithm;
  private int sessionTime;

  @Autowired
  public DefaultJwtService(
      @Value("${jwt.secret}") String secret, @Value("${jwt.sessionTime}") int sessionTime) {
    this.sessionTime = sessionTime;
    byte[] keyBytes = secret.getBytes();
    String jcaName;
    if (keyBytes.length >= 64) {
      this.algorithm = Jwts.SIG.HS512;
      jcaName = "HmacSHA512";
    } else if (keyBytes.length >= 48) {
      this.algorithm = Jwts.SIG.HS384;
      jcaName = "HmacSHA384";
    } else {
      this.algorithm = Jwts.SIG.HS256;
      jcaName = "HmacSHA256";
    }
    this.signingKey = new SecretKeySpec(keyBytes, jcaName);
  }

  @Override
  public String toToken(User user) {
    return Jwts.builder()
        .subject(user.getId())
        .expiration(expireTimeFromNow())
        .signWith(signingKey, algorithm)
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
