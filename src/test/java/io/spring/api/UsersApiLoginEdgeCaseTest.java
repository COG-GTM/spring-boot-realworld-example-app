package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.UserQueryService;
import io.spring.application.user.UserService;
import io.spring.core.service.JwtService;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UsersApi.class)
@Import({
  WebSecurityConfig.class,
  UserQueryService.class,
  BCryptPasswordEncoder.class,
  JacksonCustomizations.class
})
public class UsersApiLoginEdgeCaseTest {

  @Autowired private MockMvc mvc;

  @MockBean private UserRepository userRepository;

  @MockBean private JwtService jwtService;

  @MockBean private UserReadService userReadService;

  @MockBean private UserService userService;

  @BeforeEach
  public void setUp() {
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_fail_login_with_non_existent_email() {
    when(userRepository.findByEmail(eq("nonexistent@test.com"))).thenReturn(Optional.empty());

    Map<String, Object> param = prepareLoginParam("nonexistent@test.com", "password");

    given()
        .contentType("application/json")
        .body(param)
        .when()
        .post("/users/login")
        .then()
        .statusCode(422)
        .body("message", equalTo("invalid email or password"));
  }

  @Test
  public void should_fail_login_with_empty_email() {
    Map<String, Object> param = prepareLoginParam("", "password");

    given()
        .contentType("application/json")
        .body(param)
        .when()
        .post("/users/login")
        .then()
        .statusCode(422);
  }

  @Test
  public void should_fail_login_with_empty_password() {
    Map<String, Object> param = prepareLoginParam("test@test.com", "");

    given()
        .contentType("application/json")
        .body(param)
        .when()
        .post("/users/login")
        .then()
        .statusCode(422);
  }

  @Test
  public void should_fail_login_with_invalid_email_format() {
    Map<String, Object> param = prepareLoginParam("not-an-email", "password");

    given()
        .contentType("application/json")
        .body(param)
        .when()
        .post("/users/login")
        .then()
        .statusCode(422);
  }

  @Test
  public void should_fail_registration_with_blank_password() {
    Map<String, Object> param =
        new HashMap<String, Object>() {
          {
            put(
                "user",
                new HashMap<String, Object>() {
                  {
                    put("email", "test@test.com");
                    put("username", "testuser");
                    put("password", "");
                  }
                });
          }
        };

    given()
        .contentType("application/json")
        .body(param)
        .when()
        .post("/users")
        .then()
        .statusCode(422)
        .body("errors.password[0]", equalTo("can't be empty"));
  }

  @Test
  public void should_fail_registration_with_missing_all_fields() {
    Map<String, Object> param =
        new HashMap<String, Object>() {
          {
            put("user", new HashMap<String, Object>());
          }
        };

    given()
        .contentType("application/json")
        .body(param)
        .when()
        .post("/users")
        .then()
        .statusCode(422);
  }

  private Map<String, Object> prepareLoginParam(String email, String password) {
    return new HashMap<String, Object>() {
      {
        put(
            "user",
            new HashMap<String, Object>() {
              {
                put("email", email);
                put("password", password);
              }
            });
      }
    };
  }
}
