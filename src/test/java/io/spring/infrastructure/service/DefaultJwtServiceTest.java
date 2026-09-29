package io.spring.infrastructure.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

  private static final String SIXTY_FOUR_BYTE_SECRET =
      "nRvyYC4soFxBdZ-F-5Nnzz5USXstR1YylsTd-mA0aKtI9HUlriGrtkf-TiuDapkLiUCogO3JOK7kwZisrHp6wA";

  @Test
  public void should_accept_token_issued_by_previous_jjwt_implementation() {
    // HS512, sub=legacy-user-id, exp=2100-01-01, signed by jjwt 0.11.2 with SIXTY_FOUR_BYTE_SECRET
    String legacyToken =
        "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJsZWdhY3ktdXNlci1pZCIsImV4cCI6NDEwMjQ0NDgwMH0.cVx2LD_EXJLUvOLYDFFUjI9FxS95iSm_6uvbFt_PF5y45Gcn0svgXPaQaRMZ-5jZEmZwZEclM_ViwSAqbEbCcQ";
    JwtService service = new DefaultJwtService(SIXTY_FOUR_BYTE_SECRET, 3600);
    Assertions.assertEquals(Optional.of("legacy-user-id"), service.getSubFromToken(legacyToken));
  }

  @Test
  public void should_issue_hs512_token_with_only_sub_and_exp_claims() throws Exception {
    JwtService service = new DefaultJwtService(SIXTY_FOUR_BYTE_SECRET, 3600);
    User user = new User("email@email.com", "username", "123", "", "");
    String[] parts = service.toToken(user).split("\\.");
    Assertions.assertEquals(3, parts.length);
    Assertions.assertEquals("{\"alg\":\"HS512\"}", decode(parts[0]));

    JsonNode claims = new ObjectMapper().readTree(decode(parts[1]));
    List<String> claimNames = new ArrayList<>();
    claims.fieldNames().forEachRemaining(claimNames::add);
    Assertions.assertEquals(Set.of("sub", "exp"), Set.copyOf(claimNames));
    Assertions.assertEquals(user.getId(), claims.get("sub").asText());
    long expectedExp = System.currentTimeMillis() / 1000 + 3600;
    Assertions.assertTrue(Math.abs(claims.get("exp").asLong() - expectedExp) <= 5);
  }

  @Test
  public void should_reject_token_signed_with_another_secret() {
    JwtService other = new DefaultJwtService(SIXTY_FOUR_BYTE_SECRET, 3600);
    String token = other.toToken(new User("email@email.com", "username", "123", "", ""));
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  private static String decode(String base64Url) {
    return new String(Base64.getUrlDecoder().decode(base64Url), StandardCharsets.UTF_8);
  }
}
