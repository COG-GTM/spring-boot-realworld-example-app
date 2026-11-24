package io.spring.infrastructure.service;

import io.jsonwebtoken.Jwts;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Base64;
import java.util.Date;
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
  public void should_get_null_with_tampered_jwt() {
    User user = new User("email@email.com", "username", "123", "", "");
    String token = jwtService.toToken(user);
    String[] parts = token.split("\\.");
    String forgedPayload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("{\"sub\":\"attacker\",\"exp\":9999999999}".getBytes());
    String tampered = parts[0] + "." + forgedPayload + "." + parts[2];
    Assertions.assertFalse(jwtService.getSubFromToken(tampered).isPresent());
  }

  @Test
  public void should_get_null_with_jwt_signed_by_another_secret() {
    JwtService otherService =
        new DefaultJwtService("456456456456456456456456456456456456456456456456456456456456", 3600);
    User user = new User("email@email.com", "username", "123", "", "");
    String token = otherService.toToken(user);
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_get_null_with_unsigned_jwt() {
    String unsigned =
        Jwts.builder()
            .setSubject("attacker")
            .setExpiration(new Date(System.currentTimeMillis() + 3600_000L))
            .compact();
    Assertions.assertFalse(jwtService.getSubFromToken(unsigned).isPresent());
  }
}
