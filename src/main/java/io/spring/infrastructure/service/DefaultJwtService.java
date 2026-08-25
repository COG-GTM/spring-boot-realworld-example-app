package io.spring.infrastructure.service;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.MacAlgorithm;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultJwtService implements JwtService {
  private static final MacAlgorithm SIGNATURE_ALGORITHM = Jwts.SIG.HS512;

  private final SecretKey signingKey;
  private int sessionTime;

  @Autowired
  public DefaultJwtService(
      @Value("${jwt.secret}") String secret, @Value("${jwt.sessionTime}") int sessionTime) {
    this.sessionTime = sessionTime;
    this.signingKey = signingKeyFor(secret);
  }

  private static SecretKey signingKeyFor(String secret) {
    try {
      // HS512 requires a key of at least 512 bits, so the configured secret is expanded
      // to a fixed 64 byte key.
      byte[] keyBytes = MessageDigest.getInstance("SHA-512").digest(secret.getBytes(UTF_8));
      return new SecretKeySpec(keyBytes, "HmacSHA512");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String toToken(User user) {
    return Jwts.builder()
        .subject(user.getId())
        .expiration(expireTimeFromNow())
        .signWith(signingKey, SIGNATURE_ALGORITHM)
        .compact();
  }

  @Override
  public Optional<String> getSubFromToken(String token) {
    try {
      Jws<Claims> claimsJws = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
      return Optional.ofNullable(claimsJws.getPayload().getSubject());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private Date expireTimeFromNow() {
    return new Date(System.currentTimeMillis() + sessionTime * 1000L);
  }
}
