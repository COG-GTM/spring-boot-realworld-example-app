package io.spring.infrastructure.service;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultJwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  public void setUp() {
    jwtService = new DefaultJwtService("123123123123123123123123123123123123123123123123123123123123", 3600);
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
  public void should_accept_hs512_token_issued_before_jjwt_upgrade() {
    // Issued by jjwt 0.11.2 (HS512, 64-byte secret, expires 2100-01-01).
    String legacyToken =
        "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJsZWdhY3ktdXNlci1pZCIsImV4cCI6NDEwMjQ0NDgwMH0.pziqg4gVU3s2dF0f8EKPASx-ZZSR8fmoLZZgxj_N6m_jF4i8ynyLKg_U2kYtlCwXpZCG94xttuERn777lFacIw";
    JwtService service =
        new DefaultJwtService(
            "1231231231231231231231231231231231231231231231231231231231231231", 3600);
    Assertions.assertEquals(Optional.of("legacy-user-id"), service.getSubFromToken(legacyToken));
  }
}
