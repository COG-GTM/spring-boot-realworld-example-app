package io.spring.infrastructure.service;

import io.jsonwebtoken.Jwts;
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

  private static final String SECRET =
      "123123123123123123123123123123123123123123123123123123123123";

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
  public void should_accept_hs512_token_signed_with_raw_secret_bytes() {
    String token =
        Jwts.builder()
            .subject("legacy-user")
            .expiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(
                new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512"),
                Jwts.SIG.HS512)
            .compact();

    Assertions.assertEquals(Optional.of("legacy-user"), jwtService.getSubFromToken(token));
  }

  @Test
  public void should_reject_token_signed_with_different_secret() {
    JwtService otherService =
        new DefaultJwtService("456456456456456456456456456456456456456456456456456456456456", 3600);
    String token = otherService.toToken(new User("email@email.com", "username", "123", "", ""));

    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_reject_unsigned_token() {
    String token =
        Jwts.builder()
            .subject("attacker")
            .expiration(new Date(System.currentTimeMillis() + 60_000))
            .compact();

    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_reject_token_with_tampered_payload() {
    User user = new User("email@email.com", "username", "123", "", "");
    String[] parts = jwtService.toToken(user).split("\\.");
    String forgedPayload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                "{\"sub\":\"someone-else\",\"exp\":9999999999}".getBytes(StandardCharsets.UTF_8));
    String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

    Assertions.assertFalse(jwtService.getSubFromToken(tampered).isPresent());
  }

  @Test
  public void should_reject_secret_shorter_than_256_bits() {
    Assertions.assertThrows(WeakKeyException.class, () -> new DefaultJwtService("too-short", 3600));
  }
}
