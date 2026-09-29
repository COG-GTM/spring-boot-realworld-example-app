package io.spring.infrastructure.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.WeakKeyException;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultJwtServiceTest {

  static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

  private JwtService jwtService;

  @BeforeEach
  public void setUp() {
    jwtService = new DefaultJwtService(SECRET, 3600);
  }

  @Test
  public void should_generate_and_parse_token() {
    User user = new User("email@email.com", "username", "123", "", "");
    String token = jwtService.toToken(user);
    Assertions.assertNotNull(token);
    Optional<String> optional = jwtService.getSubFromToken(token);
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals(optional.get(), user.getId());
  }

  @Test
  public void should_get_null_with_wrong_jwt() {
    Optional<String> optional = jwtService.getSubFromToken("123");
    Assertions.assertFalse(optional.isPresent());
  }

  @Test
  public void should_get_null_with_expired_jwt() {
    String token =
        "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhaXNlbnNpeSIsImV4cCI6MTUwMjE2MTIwNH0.SJB-U60WzxLYNomqLo4G3v3LzFxJKuVrIud8D8Lz3-mgpo9pN1i7C8ikU_jQPJGm8HsC1CquGMI-rSuM7j6LDA";
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_sign_issued_tokens_with_hs512_and_set_expiry() {
    User user = new User("email@email.com", "username", "123", "", "");
    long before = System.currentTimeMillis();
    String token = jwtService.toToken(user);

    String header =
        new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
    Assertions.assertTrue(header.contains("\"alg\":\"HS512\""), header);

    Claims claims =
        Jwts.parserBuilder().setSigningKey(hmacKey()).build().parseClaimsJws(token).getBody();
    Assertions.assertNotNull(claims.getIssuedAt());
    long expiresInMillis = claims.getExpiration().getTime() - before;
    Assertions.assertTrue(expiresInMillis > 3590_000L && expiresInMillis <= 3600_000L);
  }

  @Test
  public void should_reject_expired_token_signed_with_our_key() {
    String token =
        Jwts.builder()
            .setSubject("user-id")
            .setExpiration(new Date(System.currentTimeMillis() - 1000L))
            .signWith(hmacKey(), SignatureAlgorithm.HS512)
            .compact();
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_reject_token_without_expiry() {
    String token =
        Jwts.builder()
            .setSubject("user-id")
            .signWith(hmacKey(), SignatureAlgorithm.HS512)
            .compact();
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_reject_token_signed_with_different_hmac_algorithm() {
    String token =
        Jwts.builder()
            .setSubject("user-id")
            .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
            .signWith(hmacKey(), SignatureAlgorithm.HS256)
            .compact();
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_reject_unsigned_alg_none_token() {
    String token =
        Jwts.builder()
            .setSubject("user-id")
            .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
            .compact();
    Assertions.assertTrue(token.endsWith("."));
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_reject_token_signed_with_another_key() {
    String token =
        Jwts.builder()
            .setSubject("user-id")
            .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
            .signWith(
                new SecretKeySpec(
                    SECRET.toUpperCase().getBytes(StandardCharsets.UTF_8), "HmacSHA512"),
                SignatureAlgorithm.HS512)
            .compact();
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_refuse_secret_too_short_for_hs512() {
    Assertions.assertThrows(
        WeakKeyException.class, () -> new DefaultJwtService("too-short-secret", 3600));
  }

  @Test
  public void should_refuse_non_positive_session_time() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new DefaultJwtService(SECRET, 0));
  }

  private static SecretKeySpec hmacKey() {
    return new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
  }
}
