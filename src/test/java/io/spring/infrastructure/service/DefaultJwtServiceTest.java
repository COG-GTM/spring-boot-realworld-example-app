package io.spring.infrastructure.service;

import io.jsonwebtoken.security.WeakKeyException;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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

  private static final String PRODUCTION_LENGTH_SECRET =
      "nRvyYC4soFxBdZ-F-5Nnzz5USXstR1YylsTd-mA0aKtI9HUlriGrtkf-TiuDapkLiUCogO3JOK7kwZisrHp6wA";

  @Test
  public void should_reject_token_signed_with_different_secret() {
    User user = new User("email@email.com", "username", "123", "", "");
    String foreignToken = new DefaultJwtService(PRODUCTION_LENGTH_SECRET, 3600).toToken(user);
    Assertions.assertFalse(jwtService.getSubFromToken(foreignToken).isPresent());
  }

  @Test
  public void should_reject_tampered_token() {
    User user = new User("email@email.com", "username", "123", "", "");
    String[] parts = jwtService.toToken(user).split("\\.");
    String forgedPayload =
        base64Url("{\"sub\":\"someone-else\",\"exp\":" + futureEpochSeconds() + "}");
    String tampered = parts[0] + "." + forgedPayload + "." + parts[2];
    Assertions.assertFalse(jwtService.getSubFromToken(tampered).isPresent());
  }

  @Test
  public void should_reject_unsigned_token() {
    String unsigned =
        base64Url("{\"alg\":\"none\"}")
            + "."
            + base64Url("{\"sub\":\"someone\",\"exp\":" + futureEpochSeconds() + "}")
            + ".";
    Assertions.assertFalse(jwtService.getSubFromToken(unsigned).isPresent());
  }

  @Test
  public void should_sign_with_hs512_for_production_length_secret() {
    User user = new User("email@email.com", "username", "123", "", "");
    String token = new DefaultJwtService(PRODUCTION_LENGTH_SECRET, 3600).toToken(user);
    String header =
        new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
    Assertions.assertTrue(header.contains("\"alg\":\"HS512\""), header);
  }

  @Test
  public void should_accept_hs512_token_issued_before_upgrade() throws Exception {
    String header = base64Url("{\"alg\":\"HS512\"}");
    String payload = base64Url("{\"sub\":\"legacy-user\",\"exp\":" + futureEpochSeconds() + "}");
    Mac mac = Mac.getInstance("HmacSHA512");
    mac.init(
        new SecretKeySpec(PRODUCTION_LENGTH_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
    String signature =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                mac.doFinal((header + "." + payload).getBytes(StandardCharsets.US_ASCII)));
    String legacyToken = header + "." + payload + "." + signature;

    Optional<String> sub =
        new DefaultJwtService(PRODUCTION_LENGTH_SECRET, 3600).getSubFromToken(legacyToken);
    Assertions.assertEquals(Optional.of("legacy-user"), sub);
  }

  @Test
  public void should_fail_fast_when_secret_is_shorter_than_256_bits() {
    Assertions.assertThrows(WeakKeyException.class, () -> new DefaultJwtService("too-short", 3600));
  }

  private static long futureEpochSeconds() {
    return System.currentTimeMillis() / 1000 + 3600;
  }

  private static String base64Url(String json) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(json.getBytes(StandardCharsets.UTF_8));
  }
}
