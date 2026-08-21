package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.exception.InvalidPasswordResetTokenException;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.user.PasswordResetService;
import io.spring.core.service.JwtService;
import io.spring.core.user.PasswordResetToken;
import io.spring.core.user.UserRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PasswordResetApi.class)
@Import({WebSecurityConfig.class, BCryptPasswordEncoder.class, JacksonCustomizations.class})
public class PasswordResetApiTest {
  @Autowired private MockMvc mvc;

  @MockBean private PasswordResetService passwordResetService;

  @MockBean private UserRepository userRepository;

  @MockBean private JwtService jwtService;

  @BeforeEach
  public void setUp() throws Exception {
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_return_reset_token_for_known_email() throws Exception {
    String email = "john@jacob.com";
    PasswordResetToken token = new PasswordResetToken("123", new DateTime().plusHours(1));
    when(passwordResetService.requestReset(eq(email))).thenReturn(Optional.of(token));

    given()
        .contentType("application/json")
        .body(requestParam(email))
        .when()
        .post("/users/password-reset")
        .then()
        .statusCode(200)
        .body("passwordReset.token", equalTo(token.getToken()));
  }

  @Test
  public void should_not_leak_whether_email_exists() throws Exception {
    String email = "unknown@jacob.com";
    when(passwordResetService.requestReset(eq(email))).thenReturn(Optional.empty());

    given()
        .contentType("application/json")
        .body(requestParam(email))
        .when()
        .post("/users/password-reset")
        .then()
        .statusCode(200)
        .body("passwordReset.token", equalTo(null));
  }

  @Test
  public void should_show_error_message_for_invalid_email() throws Exception {
    given()
        .contentType("application/json")
        .body(requestParam("johnxjacob.com"))
        .when()
        .post("/users/password-reset")
        .prettyPeek()
        .then()
        .statusCode(422)
        .body("errors.email[0]", equalTo("should be an email"));
  }

  @Test
  public void should_reset_password_with_valid_token() throws Exception {
    given()
        .contentType("application/json")
        .body(resetParam("valid-token", "new-password"))
        .when()
        .put("/users/password-reset")
        .then()
        .statusCode(204);

    verify(passwordResetService).resetPassword(eq("valid-token"), eq("new-password"));
  }

  @Test
  public void should_show_error_for_invalid_token() throws Exception {
    doThrow(new InvalidPasswordResetTokenException())
        .when(passwordResetService)
        .resetPassword(eq("expired-token"), any());

    given()
        .contentType("application/json")
        .body(resetParam("expired-token", "new-password"))
        .when()
        .put("/users/password-reset")
        .then()
        .statusCode(422);
  }

  @Test
  public void should_show_error_message_for_blank_password() throws Exception {
    given()
        .contentType("application/json")
        .body(resetParam("valid-token", ""))
        .when()
        .put("/users/password-reset")
        .prettyPeek()
        .then()
        .statusCode(422)
        .body("errors.password[0]", equalTo("can't be empty"));
  }

  private Map<String, Object> requestParam(final String email) {
    return new HashMap<String, Object>() {
      {
        put(
            "passwordReset",
            new HashMap<String, Object>() {
              {
                put("email", email);
              }
            });
      }
    };
  }

  private Map<String, Object> resetParam(final String token, final String password) {
    return new HashMap<String, Object>() {
      {
        put(
            "passwordReset",
            new HashMap<String, Object>() {
              {
                put("token", token);
                put("password", password);
              }
            });
      }
    };
  }
}
