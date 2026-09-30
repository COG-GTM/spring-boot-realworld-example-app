package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.application.user.UserService;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({CurrentUserApi.class, UsersApi.class})
@Import({
  WebSecurityConfig.class,
  JacksonCustomizations.class,
  UserService.class,
  UserQueryService.class,
  ValidationAutoConfiguration.class,
  BCryptPasswordEncoder.class
})
public class PasswordChangeLoginTest {
  private static final String EMAIL = "john@jacob.com";
  private static final String USERNAME = "johnjacob";
  private static final String OLD_PASSWORD = "old-password";
  private static final String NEW_PASSWORD = "new-password";
  private static final String TOKEN = "token";

  @Autowired private MockMvc mvc;

  @Autowired private PasswordEncoder passwordEncoder;

  @MockBean private UserRepository userRepository;

  @MockBean private UserReadService userReadService;

  @MockBean private JwtService jwtService;

  @BeforeEach
  public void setUp() {
    RestAssuredMockMvc.mockMvc(mvc);

    User user = new User(EMAIL, USERNAME, passwordEncoder.encode(OLD_PASSWORD), "", "");
    when(userRepository.findByEmail(eq(EMAIL))).thenReturn(Optional.of(user));
    when(userRepository.findByUsername(eq(USERNAME))).thenReturn(Optional.of(user));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));
    when(userReadService.findById(eq(user.getId())))
        .thenReturn(new UserData(user.getId(), EMAIL, USERNAME, "", ""));
    when(jwtService.getSubFromToken(eq(TOKEN))).thenReturn(Optional.of(user.getId()));
    when(jwtService.toToken(any())).thenReturn(TOKEN);
  }

  @Test
  public void should_login_with_new_password_after_password_change() {
    given()
        .contentType("application/json")
        .header("Authorization", "Token " + TOKEN)
        .body(userBody("password", NEW_PASSWORD))
        .when()
        .put("/user")
        .then()
        .statusCode(200);

    given()
        .contentType("application/json")
        .body(userBody("email", EMAIL, "password", NEW_PASSWORD))
        .when()
        .post("/users/login")
        .then()
        .statusCode(200)
        .body("user.email", equalTo(EMAIL));

    given()
        .contentType("application/json")
        .body(userBody("email", EMAIL, "password", OLD_PASSWORD))
        .when()
        .post("/users/login")
        .then()
        .statusCode(422);
  }

  private static Map<String, Object> userBody(String... keyValues) {
    Map<String, Object> fields = new HashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      fields.put(keyValues[i], keyValues[i + 1]);
    }
    Map<String, Object> body = new HashMap<>();
    body.put("user", fields);
    return body;
  }
}
