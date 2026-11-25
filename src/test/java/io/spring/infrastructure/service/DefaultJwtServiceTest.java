package io.spring.infrastructure.service;

import io.jsonwebtoken.security.WeakKeyException;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
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
  public void should_sign_with_hs512_when_secret_is_at_least_512_bits() {
    JwtService service =
        new DefaultJwtService(
            "nRvyYC4soFxBdZ-F-5Nnzz5USXstR1YylsTd-mA0aKtI9HUlriGrtkf-TiuDapkLiUCogO3JOK7kwZisrHp6wA",
            3600);
    User user = new User("email@email.com", "username", "123", "", "");
    String token = service.toToken(user);
    String header =
        new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
    Assertions.assertTrue(header.contains("\"HS512\""), header);
    Assertions.assertEquals(Optional.of(user.getId()), service.getSubFromToken(token));
  }

  @Test
  public void should_get_null_with_token_signed_by_another_secret() {
    JwtService other =
        new DefaultJwtService("456456456456456456456456456456456456456456456456456456456456", 3600);
    User user = new User("email@email.com", "username", "123", "", "");
    Assertions.assertFalse(jwtService.getSubFromToken(other.toToken(user)).isPresent());
  }

  @Test
  public void should_get_null_with_tampered_payload() {
    User user = new User("email@email.com", "username", "123", "", "");
    String[] parts = jwtService.toToken(user).split("\\.");
    String forgedPayload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                "{\"sub\":\"attacker\",\"exp\":9999999999}".getBytes(StandardCharsets.UTF_8));
    String forged = parts[0] + "." + forgedPayload + "." + parts[2];
    Assertions.assertFalse(jwtService.getSubFromToken(forged).isPresent());
  }

  @Test
  public void should_get_null_with_unsigned_token() {
    String header =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
    String payload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                "{\"sub\":\"attacker\",\"exp\":9999999999}".getBytes(StandardCharsets.UTF_8));
    Assertions.assertFalse(jwtService.getSubFromToken(header + "." + payload + ".").isPresent());
  }

  @Test
  public void should_reject_secret_shorter_than_256_bits() {
    Assertions.assertThrows(WeakKeyException.class, () -> new DefaultJwtService("too-short", 3600));
  }
}
