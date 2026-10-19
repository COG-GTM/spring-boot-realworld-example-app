package io.spring.infrastructure.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultJwtServiceTest {

  private JwtService jwtService;

  static String randomSecret() {
    byte[] bytes = new byte[64];
    new SecureRandom().nextBytes(bytes);
    return Base64.getEncoder().encodeToString(bytes);
  }

  @BeforeEach
  public void setUp() {
    jwtService = new DefaultJwtService(randomSecret(), 3600);
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
  public void should_sign_with_hs512() {
    User user = new User("email@email.com", "username", "123", "", "");
    String header = jwtService.toToken(user).split("\\.")[0];
    String decoded = new String(Base64.getUrlDecoder().decode(header), StandardCharsets.UTF_8);
    Assertions.assertTrue(decoded.contains("\"HS512\""), decoded);
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
  public void should_reject_token_signed_with_another_secret() {
    byte[] otherKey = randomSecret().getBytes(StandardCharsets.UTF_8);
    String forged =
        Jwts.builder()
            .setSubject("victim-user-id")
            .setExpiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(
                new SecretKeySpec(otherKey, SignatureAlgorithm.HS512.getJcaName()),
                SignatureAlgorithm.HS512)
            .compact();
    Assertions.assertFalse(jwtService.getSubFromToken(forged).isPresent());
  }

  @Test
  public void should_fail_fast_without_secret() {
    Assertions.assertThrows(IllegalStateException.class, () -> new DefaultJwtService(null, 3600));
    Assertions.assertThrows(IllegalStateException.class, () -> new DefaultJwtService("  ", 3600));
    Assertions.assertThrows(
        IllegalStateException.class, () -> new DefaultJwtService("${JWT_SECRET}", 3600));
  }

  @Test
  public void should_fail_fast_with_secret_shorter_than_512_bits() {
    String tooShort = "a".repeat(DefaultJwtService.MIN_SECRET_BYTES - 1);
    Assertions.assertThrows(
        IllegalStateException.class, () -> new DefaultJwtService(tooShort, 3600));
  }

  @Test
  public void should_not_commit_a_jwt_secret_value() throws Exception {
    Properties properties = new Properties();
    try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
      properties.load(in);
    }
    Assertions.assertEquals("${JWT_SECRET}", properties.getProperty("jwt.secret"));
  }
}
