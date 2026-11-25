package io.spring.infrastructure.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultJwtServiceTest {

  private static final String SECRET =
      "123123123123123123123123123123123123123123123123123123123123";
  private static final String PRODUCTION_LENGTH_SECRET =
      "nRvyYC4soFxBdZ-F-5Nnzz5USXstR1YylsTd-mA0aKtI9HUlriGrtkf-TiuDapkLiUCogO3JOK7kwZisrHp6wA";

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
  public void should_get_null_with_expired_jwt_signed_with_same_key() {
    String token =
        Jwts.builder()
            .subject("user-id")
            .expiration(new Date(System.currentTimeMillis() - 60_000))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
            .compact();
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_get_null_with_jwt_signed_by_other_secret() {
    DefaultJwtService other =
        new DefaultJwtService("456456456456456456456456456456456456456456456456456456456456", 3600);
    String token = other.toToken(new User("email@email.com", "username", "123", "", ""));
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_get_null_with_tampered_jwt() {
    User user = new User("email@email.com", "username", "123", "", "");
    String[] parts = jwtService.toToken(user).split("\\.");
    String forgedPayload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                "{\"sub\":\"attacker\",\"exp\":9999999999}".getBytes(StandardCharsets.UTF_8));
    String tampered = parts[0] + "." + forgedPayload + "." + parts[2];
    Assertions.assertFalse(jwtService.getSubFromToken(tampered).isPresent());
  }

  @Test
  public void should_get_null_with_unsigned_jwt() {
    String token = Jwts.builder().subject("user-id").compact();
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_set_expiration_from_session_time() {
    User user = new User("email@email.com", "username", "123", "", "");
    long before = System.currentTimeMillis() / 1000;
    String token = jwtService.toToken(user);
    Date expiration =
        Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getExpiration();
    long exp = expiration.getTime() / 1000;
    Assertions.assertTrue(exp >= before + 3600 && exp <= before + 3600 + 5);
  }

  @Test
  public void should_sign_with_hs512_for_production_length_secret() {
    JwtService service = new DefaultJwtService(PRODUCTION_LENGTH_SECRET, 3600);
    String token = service.toToken(new User("email@email.com", "username", "123", "", ""));
    String header =
        new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
    Assertions.assertTrue(header.contains("\"alg\":\"HS512\""), header);
  }

  @Test
  public void should_reject_secret_shorter_than_256_bits() {
    Assertions.assertThrows(WeakKeyException.class, () -> new DefaultJwtService("too-short", 3600));
  }
}
