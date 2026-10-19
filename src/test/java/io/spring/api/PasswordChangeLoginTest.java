package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.UserQueryService;
import io.spring.application.user.UserService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({CurrentUserApi.class, UsersApi.class})
@Import({
  WebSecurityConfig.class,
  JacksonCustomizations.class,
  UserService.class,
  ValidationAutoConfiguration.class,
  BCryptPasswordEncoder.class
})
public class PasswordChangeLoginTest extends TestWithCurrentUser {

  @Autowired private MockMvc mvc;

  @MockBean private UserQueryService userQueryService;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));
    when(jwtService.toToken(any())).thenReturn(token);
  }

  @Test
  public void should_store_hash_and_login_with_new_password_after_update() throws Exception {
    Map<String, Object> changes = new HashMap<>();
    changes.put("password", "newpw");
    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(wrap(changes))
        .when()
        .put("/user")
        .then()
        .statusCode(200);

    assertNotEquals("newpw", user.getPassword());

    given()
        .contentType("application/json")
        .body(wrap(credentials("newpw")))
        .when()
        .post("/users/login")
        .then()
        .statusCode(200)
        .body("user.email", equalTo(email));

    given()
        .contentType("application/json")
        .body(wrap(credentials("123")))
        .when()
        .post("/users/login")
        .then()
        .statusCode(422);
  }

  private Map<String, Object> credentials(String password) {
    Map<String, Object> credentials = new HashMap<>();
    credentials.put("email", email);
    credentials.put("password", password);
    return credentials;
  }

  private Map<String, Object> wrap(Map<String, Object> body) {
    Map<String, Object> wrapped = new HashMap<>();
    wrapped.put("user", body);
    return wrapped;
  }
}
