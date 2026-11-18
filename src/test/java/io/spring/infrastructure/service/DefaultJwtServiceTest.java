package io.spring.infrastructure.service;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.jsonwebtoken.security.WeakKeyException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultJwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  public void setUp() {
    jwtService =
        new DefaultJwtService("123123123123123123123123123123123123123123123123123123123123", 3600);
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
  public void should_parse_hs512_token_issued_by_jjwt_0_11() {
    // signed by jjwt 0.11.2 with a >= 64-byte secret (HS512, like jwt.secret), exp 2100-01-01
    JwtService service =
        new DefaultJwtService(
            "123123123123123123123123123123123123123123123123123123123123123123123123", 3600);
    String token =
        "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyLWlkIiwiZXhwIjo0MTAyNDQ0ODAwfQ.Eil4DMOeaFtXhYo7Zi1I0TBkxgUtkq-1YF0x1j-MoOlj1HmX9LclnwFcI52vPhYFPAAqY7-CDcfjcbA4fS0_Ww";
    Assertions.assertEquals(Optional.of("user-id"), service.getSubFromToken(token));
  }

  private static String headerOf(String token) {
    return new String(
        Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
  }

  @Test
  public void should_sign_with_hs512_when_secret_is_at_least_64_bytes() {
    JwtService service =
        new DefaultJwtService(
            "nRvyYC4soFxBdZ-F-5Nnzz5USXstR1YylsTd-mA0aKtI9HUlriGrtkf-TiuDapkLiUCogO3JOK7kwZisrHp6wA",
            3600);
    String token = service.toToken(new User("email@email.com", "username", "123", "", ""));
    Assertions.assertTrue(headerOf(token).contains("\"HS512\""), headerOf(token));
  }

  @Test
  public void should_pick_hmac_algorithm_from_secret_length() {
    String token = jwtService.toToken(new User("email@email.com", "username", "123", "", ""));
    // 60-byte secret: jjwt 0.12 selects HS384 (jjwt 0.11 config always used HS512)
    Assertions.assertTrue(headerOf(token).contains("\"HS384\""), headerOf(token));
  }

  @Test
  public void should_reject_secret_shorter_than_256_bits() {
    Assertions.assertThrows(
        WeakKeyException.class, () -> new DefaultJwtService("too-short-secret", 3600));
  }

  @Test
  public void should_reject_token_signed_with_another_secret() {
    JwtService other =
        new DefaultJwtService(
            "456456456456456456456456456456456456456456456456456456456456", 3600);
    String token = other.toToken(new User("email@email.com", "username", "123", "", ""));
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_reject_token_issued_with_expired_session_time() {
    JwtService expiring =
        new DefaultJwtService(
            "123123123123123123123123123123123123123123123123123123123123", -60);
    String token = expiring.toToken(new User("email@email.com", "username", "123", "", ""));
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }
}
