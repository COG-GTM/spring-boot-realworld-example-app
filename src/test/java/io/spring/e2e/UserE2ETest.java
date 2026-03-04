package io.spring.e2e;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public class UserE2ETest extends BaseE2ETest {

  @Test
  void should_register_user_successfully() {
    Map<String, Object> param = userParam("register-test@test.com", "registeruser", "password123");
    ResponseEntity<String> response = post("/users", param);

    assertEquals(HttpStatus.CREATED.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    assertEquals("register-test@test.com", body.get("user").get("email").asText());
    assertEquals("registeruser", body.get("user").get("username").asText());
    assertNotNull(body.get("user").get("token").asText());
    assertFalse(body.get("user").get("token").asText().isEmpty());
  }

  @Test
  void should_login_successfully_after_registration() {
    String email = "login-test@test.com";
    String password = "password123";
    registerAndGetToken(email, "loginuser", password);

    Map<String, Object> loginBody = loginParam(email, password);
    ResponseEntity<String> response = post("/users/login", loginBody);

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    assertEquals(email, body.get("user").get("email").asText());
    assertNotNull(body.get("user").get("token").asText());
    assertFalse(body.get("user").get("token").asText().isEmpty());
  }

  @Test
  void should_fail_login_with_wrong_password() {
    String email = "wrongpw-test@test.com";
    registerAndGetToken(email, "wrongpwuser", "password123");

    Map<String, Object> loginBody = loginParam(email, "wrongpassword");
    HttpClientErrorException ex =
        expectClientError(HttpMethod.POST, "/users/login", loginBody, null);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getRawStatusCode());
  }

  @Test
  void should_fail_register_with_blank_username() {
    Map<String, Object> param = userParam("blankuser@test.com", "", "password123");
    HttpClientErrorException ex = expectClientError(HttpMethod.POST, "/users", param, null);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getRawStatusCode());
  }

  @Test
  void should_fail_register_with_invalid_email() {
    Map<String, Object> param = userParam("invalid-email", "invalidemail", "password123");
    HttpClientErrorException ex = expectClientError(HttpMethod.POST, "/users", param, null);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getRawStatusCode());
  }

  @Test
  void should_fail_register_with_duplicate_email() {
    String email = "dupemail-test@test.com";
    registerAndGetToken(email, "dupemailuser1", "password123");

    Map<String, Object> param = userParam(email, "dupemailuser2", "password123");
    HttpClientErrorException ex = expectClientError(HttpMethod.POST, "/users", param, null);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getRawStatusCode());
  }

  @Test
  void should_fail_register_with_duplicate_username() {
    registerAndGetToken("dupname1@test.com", "dupnameuser", "password123");

    Map<String, Object> param = userParam("dupname2@test.com", "dupnameuser", "password123");
    HttpClientErrorException ex = expectClientError(HttpMethod.POST, "/users", param, null);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getRawStatusCode());
  }

  @Test
  void should_access_tags_endpoint_via_selenium_driver() {
    String pageSource = navigateAndGetSource("/tags");
    assertNotNull(pageSource);
    assertTrue(pageSource.contains("tags"));
  }
}
