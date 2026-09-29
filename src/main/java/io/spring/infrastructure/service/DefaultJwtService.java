package io.spring.infrastructure.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.UnsupportedJwtException;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultJwtService implements JwtService {
  static final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS512;

  private final SecretKey signingKey;
  private final JwtParser parser;
  private final int sessionTime;

  @Autowired
  public DefaultJwtService(
      @Value("${jwt.secret}") String secret, @Value("${jwt.sessionTime}") int sessionTime) {
    if (sessionTime <= 0) {
      throw new IllegalArgumentException("jwt.sessionTime must be a positive number of seconds");
    }
    this.sessionTime = sessionTime;
    this.signingKey =
        new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8), SIGNATURE_ALGORITHM.getJcaName());
    SIGNATURE_ALGORITHM.assertValidSigningKey(signingKey);
    this.parser =
        Jwts.parserBuilder()
            .setSigningKeyResolver(
                new SigningKeyResolverAdapter() {
                  @Override
                  public Key resolveSigningKey(JwsHeader header, Claims claims) {
                    if (!SIGNATURE_ALGORITHM.getValue().equals(header.getAlgorithm())) {
                      throw new UnsupportedJwtException(
                          "Unexpected JWT signing algorithm: " + header.getAlgorithm());
                    }
                    return signingKey;
                  }
                })
            .build();
  }

  @Override
  public String toToken(User user) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .setSubject(user.getId())
        .setIssuedAt(new Date(now))
        .setExpiration(new Date(now + sessionTime * 1000L))
        .signWith(signingKey, SIGNATURE_ALGORITHM)
        .compact();
  }

  @Override
  public Optional<String> getSubFromToken(String token) {
    try {
      Claims claims = parser.parseClaimsJws(token).getBody();
      if (claims.getExpiration() == null) {
        return Optional.empty();
      }
      return Optional.ofNullable(claims.getSubject());
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
