package io.spring.e2e;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public class CurrentUserE2ETest extends BaseE2ETest {

  @Test
  void should_get_current_user() {
    String token = registerAndGetToken("currentuser@test.com", "currentuser", "password123");

    ResponseEntity<String> response = get("/user", token);

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    assertEquals("currentuser@test.com", body.get("user").get("email").asText());
    assertEquals("currentuser", body.get("user").get("username").asText());
  }

  @Test
  void should_update_current_user_bio() {
    String token = registerAndGetToken("updatebio@test.com", "updatebiouser", "password123");

    Map<String, Object> user = new HashMap<>();
    user.put("bio", "Updated bio text");
    Map<String, Object> param = new HashMap<>();
    param.put("user", user);

    ResponseEntity<String> response = put("/user", param, token);

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    assertEquals("Updated bio text", body.get("user").get("bio").asText());
  }

  @Test
  void should_update_current_user_email() {
    String token = registerAndGetToken("oldemail@test.com", "emailupdater", "password123");

    Map<String, Object> user = new HashMap<>();
    user.put("email", "newemail@test.com");
    Map<String, Object> param = new HashMap<>();
    param.put("user", user);

    ResponseEntity<String> response = put("/user", param, token);

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    assertEquals("newemail@test.com", body.get("user").get("email").asText());
  }

  @Test
  void should_fail_get_current_user_without_auth() {
    HttpClientErrorException ex = expectClientError(HttpMethod.GET, "/user", null, null);

    assertEquals(HttpStatus.UNAUTHORIZED.value(), ex.getRawStatusCode());
  }
}
