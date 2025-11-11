package io.spring.graphql;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import({WebSecurityConfig.class, UserQueryService.class, BCryptPasswordEncoder.class, JacksonCustomizations.class})
public class UserMutationTest extends GraphQLTestHelper {

  @MockBean private UserRepository userRepository;

  @MockBean private JwtService jwtService;

  @MockBean private UserReadService userReadService;

  @MockBean private UserService userService;

  @Autowired private PasswordEncoder passwordEncoder;

  private String defaultAvatar;

  @BeforeEach
  public void setUp() {
    defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";
  }

  @Test
  public void should_create_user_successfully() throws Exception {
    String email = "john@example.com";
    String username = "johndoe";
    String password = "password123";

    User user = new User(email, username, "encodedPassword", "", defaultAvatar);
    UserData userData = new UserData(user.getId(), email, username, "", defaultAvatar);

    when(userService.createUser(any())).thenReturn(user);
    when(userReadService.findById(any())).thenReturn(userData);
    when(jwtService.toToken(any())).thenReturn("test-token");
    when(userRepository.findByUsername(eq(username))).thenReturn(Optional.empty());
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.empty());

    String mutation = """
        mutation CreateUser($input: CreateUserInput!) {
          createUser(input: $input) {
            ... on UserPayload {
              user {
                email
                username
                token
              }
            }
            ... on Error {
              message
              errors {
                key
                value
              }
            }
          }
        }
        """;

    Map<String, Object> variables = new HashMap<>();
    Map<String, Object> input = new HashMap<>();
    input.put("email", email);
    input.put("username", username);
    input.put("password", password);
    variables.put("input", input);

    MvcResult result = executeGraphQL(mutation, variables);
    String content = result.getResponse().getContentAsString();

    assertFalse(hasErrors(result));
    assertThat(JsonPath.read(content, "$.data.createUser.user.email"), equalTo(email));
    assertThat(JsonPath.read(content, "$.data.createUser.user.username"), equalTo(username));
    assertThat(JsonPath.read(content, "$.data.createUser.user.token"), notNullValue());

    verify(userService).createUser(any());
  }

  @Test
  public void should_return_error_for_duplicate_email() throws Exception {
    String email = "existing@example.com";
    String username = "newuser";

    when(userRepository.findByEmail(eq(email)))
        .thenReturn(Optional.of(new User(email, "existinguser", "123", "", "")));
    when(userRepository.findByUsername(eq(username))).thenReturn(Optional.empty());

    String mutation = """
        mutation CreateUser($input: CreateUserInput!) {
          createUser(input: $input) {
            ... on UserPayload {
              user {
                email
                username
              }
            }
            ... on Error {
              message
              errors {
                key
                value
              }
            }
          }
        }
        """;

    Map<String, Object> variables = new HashMap<>();
    Map<String, Object> input = new HashMap<>();
    input.put("email", email);
    input.put("username", username);
    input.put("password", "password123");
    variables.put("input", input);

    MvcResult result = executeGraphQL(mutation, variables);
    String content = result.getResponse().getContentAsString();

    Object errors = JsonPath.read(content, "$.data.createUser.errors");
    assertThat(errors, notNullValue());
  }

  @Test
  public void should_login_successfully() throws Exception {
    String email = "john@example.com";
    String username = "johndoe";
    String password = "password123";

    User user = new User(email, username, passwordEncoder.encode(password), "", defaultAvatar);
    UserData userData = new UserData(user.getId(), email, username, "", defaultAvatar);

    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(userReadService.findById(eq(user.getId()))).thenReturn(userData);
    when(jwtService.toToken(any())).thenReturn("test-token");

    String mutation = """
        mutation Login($email: String!, $password: String!) {
          login(email: $email, password: $password) {
            user {
              email
              username
              token
            }
          }
        }
        """;

    Map<String, Object> variables = new HashMap<>();
    variables.put("email", email);
    variables.put("password", password);

    MvcResult result = executeGraphQL(mutation, variables);
    String content = result.getResponse().getContentAsString();

    assertFalse(hasErrors(result));
    assertThat(JsonPath.read(content, "$.data.login.user.email"), equalTo(email));
    assertThat(JsonPath.read(content, "$.data.login.user.username"), equalTo(username));
    assertThat(JsonPath.read(content, "$.data.login.user.token"), notNullValue());
  }

  @Test
  public void should_fail_login_with_wrong_password() throws Exception {
    String email = "john@example.com";
    String username = "johndoe";
    String correctPassword = "password123";
    String wrongPassword = "wrongpassword";

    User user = new User(email, username, passwordEncoder.encode(correctPassword), "", defaultAvatar);

    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));

    String mutation = """
        mutation Login($email: String!, $password: String!) {
          login(email: $email, password: $password) {
            user {
              email
              username
              token
            }
          }
        }
        """;

    Map<String, Object> variables = new HashMap<>();
    variables.put("email", email);
    variables.put("password", wrongPassword);

    MvcResult result = executeGraphQL(mutation, variables);

    assertTrue(hasErrors(result));
  }

  @Test
  public void should_fail_login_with_nonexistent_email() throws Exception {
    String email = "nonexistent@example.com";
    String password = "password123";

    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.empty());

    String mutation = """
        mutation Login($email: String!, $password: String!) {
          login(email: $email, password: $password) {
            user {
              email
              username
              token
            }
          }
        }
        """;

    Map<String, Object> variables = new HashMap<>();
    variables.put("email", email);
    variables.put("password", password);

    MvcResult result = executeGraphQL(mutation, variables);

    assertTrue(hasErrors(result));
  }

  @Test
  public void should_update_user_successfully() throws Exception {
    String email = "john@example.com";
    String username = "johndoe";
    String newBio = "Updated bio";
    String newImage = "https://example.com/new-image.jpg";

    User user = new User(email, username, "encodedPassword", "", defaultAvatar);
    UserData userData = new UserData(user.getId(), email, username, newBio, newImage);

    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));
    when(userReadService.findById(eq(user.getId()))).thenReturn(userData);
    when(jwtService.toToken(any())).thenReturn("test-token");
    when(jwtService.getSubFromToken(any())).thenReturn(Optional.of(user.getId()));

    String mutation = """
        mutation UpdateUser($changes: UpdateUserInput!) {
          updateUser(changes: $changes) {
            user {
              email
              username
              bio
              image
            }
          }
        }
        """;

    Map<String, Object> variables = new HashMap<>();
    Map<String, Object> changes = new HashMap<>();
    changes.put("bio", newBio);
    changes.put("image", newImage);
    variables.put("changes", changes);

    MvcResult result = executeGraphQL(mutation, variables, "test-token");
    String content = result.getResponse().getContentAsString();

    assertFalse(hasErrors(result));
    assertThat(JsonPath.read(content, "$.data.updateUser.user.bio"), equalTo(newBio));
    assertThat(JsonPath.read(content, "$.data.updateUser.user.image"), equalTo(newImage));
  }
}
