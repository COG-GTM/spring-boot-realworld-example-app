package io.spring.infrastructure.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
  private final SignatureAlgorithm signatureAlgorithm;
  private int sessionTime;
  private int accessTokenExpiration;
  private int refreshTokenExpiration;

  @Autowired
  public DefaultJwtService(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.sessionTime}") int sessionTime,
      @Value("${jwt.access-token.expiration}") int accessTokenExpiration,
      @Value("${jwt.refresh-token.expiration}") int refreshTokenExpiration) {
    this.sessionTime = sessionTime;
    this.accessTokenExpiration = accessTokenExpiration;
    this.refreshTokenExpiration = refreshTokenExpiration;
    signatureAlgorithm = SignatureAlgorithm.HS512;
    this.signingKey = new SecretKeySpec(secret.getBytes(), signatureAlgorithm.getJcaName());
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
  public String generateAccessToken(User user) {
    return Jwts.builder()
        .setSubject(user.getId())
        .claim("type", "access")
        .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration * 1000L))
        .signWith(signingKey)
        .compact();
  }

  @Override
  public String generateRefreshToken(User user) {
    return Jwts.builder()
        .setSubject(user.getId())
        .claim("type", "refresh")
        .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration * 1000L))
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

  @Override
  public Optional<String> validateRefreshToken(String token) {
    try {
      Jws<Claims> claimsJws =
          Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);
      Claims claims = claimsJws.getBody();

      if (!"refresh".equals(claims.get("type", String.class))) {
        return Optional.empty();
      }

      return Optional.ofNullable(claims.getSubject());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public SecretKey getSigningKey() {
    return signingKey;
  }

  private Date expireTimeFromNow() {
    return new Date(System.currentTimeMillis() + sessionTime * 1000L);
  }
}
