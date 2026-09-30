package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.spring.JacksonCustomizations;
import io.spring.api.security.LoginAttemptLimiter;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.application.user.UserAuthenticator;
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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UsersApi.class)
@Import({
  WebSecurityConfig.class,
  UserQueryService.class,
  UserAuthenticator.class,
  LoginAttemptLimiter.class,
  BCryptPasswordEncoder.class,
  JacksonCustomizations.class
})
public class UsersApiTest {
  @Autowired private MockMvc mvc;

  @MockBean private UserRepository userRepository;

  @MockBean private JwtService jwtService;

  @MockBean private UserReadService userReadService;

  @MockBean private UserService userService;

  @Autowired private PasswordEncoder passwordEncoder;

  private String defaultAvatar;

  @BeforeEach
  public void setUp() throws Exception {
    RestAssuredMockMvc.mockMvc(mvc);
    defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";
  }

  @Test
  public void should_create_user_success() throws Exception {
    String email = "john@jacob.com";
    String username = "johnjacob";

    when(jwtService.toToken(any())).thenReturn("123");
    User user = new User(email, username, "123", "", defaultAvatar);
    UserData userData = new UserData(user.getId(), email, username, "", defaultAvatar);
    when(userReadService.findById(any())).thenReturn(userData);

    when(userService.createUser(any())).thenReturn(user);

    when(userRepository.findByUsername(eq(username))).thenReturn(Optional.empty());
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.empty());

    Map<String, Object> param = prepareRegisterParameter(email, username);

    given()
        .contentType("application/json")
        .body(param)
        .when()
        .post("/users")
        .then()
        .statusCode(201)
        .body("user.email", equalTo(email))
        .body("user.username", equalTo(username))
        .body("user.bio", equalTo(""))
        .body("user.image", equalTo(defaultAvatar))
        .body("user.token", equalTo("123"));

    verify(userService).createUser(any());
  }

  @Test
  public void should_show_error_message_for_blank_username() throws Exception {

    String email = "john@jacob.com";
    String username = "";

    Map<String, Object> param = prepareRegisterParameter(email, username);

    given()
        .contentType("application/json")
        .body(param)
        .when()
        .post("/users")
        .prettyPeek()
        .then()
        .statusCode(422)
        .body("errors.username[0]", equalTo("can't be empty"));
  }

  @Test
  public void should_show_error_message_for_invalid_email() throws Exception {
    String email = "johnxjacob.com";
    String username = "johnjacob";

    Map<String, Object> param = prepareRegisterParameter(email, username);

    given()
        .contentType("application/json")
        .body(param)
        .when()
        .post("/users")
        .prettyPeek()
        .then()
        .statusCode(422)
        .body("errors.email[0]", equalTo("should be an email"));
  }

  @Test
  public void should_show_error_for_duplicated_username() throws Exception {
    String email = "john@jacob.com";
    String username = "johnjacob";

    when(userRepository.findByUsername(eq(username)))
        .thenReturn(Optional.of(new User(email, username, "123", "bio", "")));
    when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

    Map<String, Object> param = prepareRegisterParameter(email, username);

    given()
        .contentType("application/json")
        .body(param)
        .when()
        .post("/users")
        .prettyPeek()
        .then()
        .statusCode(422)
        .body("errors.username[0]", equalTo("duplicated username"));
  }

  @Test
  public void should_show_error_for_duplicated_email() throws Exception {
    String email = "john@jacob.com";
    String username = "johnjacob2";

    when(userRepository.findByEmail(eq(email)))
        .thenReturn(Optional.of(new User(email, username, "123", "bio", "")));

    when(userRepository.findByUsername(eq(username))).thenReturn(Optional.empty());

    Map<String, Object> param = prepareRegisterParameter(email, username);

    given()
        .contentType("application/json")
        .body(param)
        .when()
        .post("/users")
        .then()
        .statusCode(422)
        .body("errors.email[0]", equalTo("duplicated email"));
  }

  private HashMap<String, Object> prepareRegisterParameter(
      final String email, final String username) {
    return new HashMap<String, Object>() {
      {
        put(
            "user",
            new HashMap<String, Object>() {
              {
                put("email", email);
                put("password", "johnnyjacob");
                put("username", username);
              }
            });
      }
    };
  }

  @Test
  public void should_login_success() throws Exception {
    String email = "john@jacob.com";
    String username = "johnjacob2";
    String password = "123";

    User user = new User(email, username, passwordEncoder.encode(password), "", defaultAvatar);
    UserData userData = new UserData("123", email, username, "", defaultAvatar);

    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(userReadService.findByUsername(eq(username))).thenReturn(userData);
    when(userReadService.findById(eq(user.getId()))).thenReturn(userData);
    when(jwtService.toToken(any())).thenReturn("123");

    Map<String, Object> param =
        new HashMap<String, Object>() {
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

    given()
        .contentType("application/json")
        .body(param)
        .when()
        .post("/users/login")
        .then()
        .statusCode(200)
        .body("user.email", equalTo(email))
        .body("user.username", equalTo(username))
        .body("user.bio", equalTo(""))
        .body("user.image", equalTo(defaultAvatar))
        .body("user.token", equalTo("123"));
    ;
  }

  @Test
  public void should_fail_login_with_wrong_password() throws Exception {
    String email = "john@jacob.com";
    String username = "johnjacob2";
    String password = "123";

    User user = new User(email, username, password, "", defaultAvatar);
    UserData userData = new UserData(user.getId(), email, username, "", defaultAvatar);

    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(userReadService.findByUsername(eq(username))).thenReturn(userData);

    Map<String, Object> param =
        new HashMap<String, Object>() {
          {
            put(
                "user",
                new HashMap<String, Object>() {
                  {
                    put("email", email);
                    put("password", "123123");
                  }
                });
          }
        };

    given()
        .contentType("application/json")
        .body(param)
        .when()
        .post("/users/login")
        .prettyPeek()
        .then()
        .statusCode(422)
        .body("message", equalTo("invalid email or password"));
  }

  @Test
  public void should_lock_account_after_repeated_failures_from_any_ip() throws Exception {
    String email = "locked@jacob.com";
    User user = new User(email, "locked", passwordEncoder.encode("right"), "", defaultAvatar);
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));

    for (int i = 0; i < 5; i++) {
      login(email, "wrong", "10.1.0." + i).then().statusCode(422);
    }

    login(email, "right", "10.1.1.1")
        .then()
        .statusCode(429)
        .header("Retry-After", Integer::parseInt, greaterThan(0))
        .body("message", equalTo("too many login attempts, try again later"));
  }

  @Test
  public void should_lock_unknown_email_the_same_way_as_existing_account() throws Exception {
    String email = "nobody@jacob.com";
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.empty());

    for (int i = 0; i < 5; i++) {
      login(email, "wrong", "10.2.0." + i)
          .then()
          .statusCode(422)
          .body("message", equalTo("invalid email or password"));
    }

    login(email, "wrong", "10.2.1.1")
        .then()
        .statusCode(429)
        .body("message", equalTo("too many login attempts, try again later"));
  }

  @Test
  public void should_block_ip_after_repeated_failures_across_accounts() throws Exception {
    String ip = "10.3.0.1";
    for (int i = 0; i < 20; i++) {
      login("stuffing" + i + "@jacob.com", "wrong", ip).then().statusCode(422);
    }

    String email = "victim@jacob.com";
    User user = new User(email, "victim", passwordEncoder.encode("right"), "", defaultAvatar);
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));

    login(email, "right", ip).then().statusCode(429);
  }

  @Test
  public void should_reset_account_failures_after_successful_login() throws Exception {
    String email = "reset@jacob.com";
    String username = "reset";
    User user = new User(email, username, passwordEncoder.encode("right"), "", defaultAvatar);
    UserData userData = new UserData(user.getId(), email, username, "", defaultAvatar);
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(userReadService.findById(eq(user.getId()))).thenReturn(userData);
    when(jwtService.toToken(any())).thenReturn("123");

    for (int i = 0; i < 4; i++) {
      login(email, "wrong", "10.4.0.1").then().statusCode(422);
    }
    login(email, "right", "10.4.0.1").then().statusCode(200);
    for (int i = 0; i < 4; i++) {
      login(email, "wrong", "10.4.0.2").then().statusCode(422);
    }
    login(email, "right", "10.4.0.2").then().statusCode(200);
  }

  private MockMvcResponse login(String email, String password, String remoteAddr) {
    Map<String, Object> param = new HashMap<>();
    Map<String, Object> credentials = new HashMap<>();
    credentials.put("email", email);
    credentials.put("password", password);
    param.put("user", credentials);
    return given()
        .contentType("application/json")
        .body(param)
        .postProcessors(
            request -> {
              request.setRemoteAddr(remoteAddr);
              return request;
            })
        .when()
        .post("/users/login");
  }
}
