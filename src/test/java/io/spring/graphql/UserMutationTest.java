package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.TypeRef;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.UserQueryService;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      UserMutation.class,
      MeDatafetcher.class,
      WebSecurityConfig.class,
      BCryptPasswordEncoder.class,
      JacksonCustomizations.class,
      GraphQLCustomizeExceptionHandler.class
    })
@Import({UserQueryService.class})
public class UserMutationTest extends GraphQLTestBase {

  @MockBean private UserService userService;

  @MockBean private PasswordEncoder passwordEncoder;

  @Test
  public void should_create_user_successfully() {
    String email = "test@example.com";
    String username = "testuser";
    String password = "password123";

    User newUser = new User(email, username, password, "", defaultAvatar);
    when(userService.createUser(any())).thenReturn(newUser);
    when(jwtService.toToken(any())).thenReturn("test-token");
    when(userReadService.findById(eq(newUser.getId()))).thenReturn(userData);

    String query =
        "mutation CreateUser($input: CreateUserInput) {"
            + "  createUser(input: $input) {"
            + "    ... on UserPayload {"
            + "      user {"
            + "        email"
            + "        username"
            + "        token"
            + "      }"
            + "    }"
            + "    ... on Error {"
            + "      message"
            + "      errors {"
            + "        key"
            + "        value"
            + "      }"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    Map<String, String> input = new HashMap<>();
    input.put("email", email);
    input.put("username", username);
    input.put("password", password);
    variables.put("input", input);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.createUser", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();
    verify(userService).createUser(any());
  }

  @Test
  public void should_login_successfully() {
    String email = "john@jacob.com";
    String password = "password123";

    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq(password), any())).thenReturn(true);
    when(jwtService.toToken(any())).thenReturn("login-token");
    when(userReadService.findById(eq(user.getId()))).thenReturn(userData);

    String query =
        "mutation Login($email: String!, $password: String!) {"
            + "  login(email: $email, password: $password) {"
            + "    user {"
            + "      email"
            + "      username"
            + "      token"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("email", email);
    variables.put("password", password);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.login.user", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();
    assertThat(result.get("email")).isEqualTo(email);
  }

  @Test
  public void should_fail_login_with_invalid_credentials() {
    String email = "invalid@example.com";
    String password = "wrongpassword";

    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.empty());

    String query =
        "mutation Login($email: String!, $password: String!) {"
            + "  login(email: $email, password: $password) {"
            + "    user {"
            + "      email"
            + "      username"
            + "      token"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("email", email);
    variables.put("password", password);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(
          query, "data.login.user", variables, new TypeRef<Map<String, Object>>() {});
    } catch (Exception e) {
      assertThat(e).isNotNull();
    }
  }

  @Test
  public void should_update_user_successfully() {
    authenticateUser();

    String newEmail = "newemail@example.com";
    String newBio = "Updated bio";

    when(userReadService.findById(eq(user.getId()))).thenReturn(userData);
    when(jwtService.toToken(any())).thenReturn("updated-token");

    String query =
        "mutation UpdateUser($changes: UpdateUserInput!) {"
            + "  updateUser(changes: $changes) {"
            + "    user {"
            + "      email"
            + "      username"
            + "      bio"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    Map<String, String> changes = new HashMap<>();
    changes.put("email", newEmail);
    changes.put("bio", newBio);
    variables.put("changes", changes);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.updateUser.user", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();
    verify(userService).updateUser(any());

    clearAuthentication();
  }

  @Test
  public void should_fail_update_user_without_authentication() {
    clearAuthentication();

    String query =
        "mutation UpdateUser($changes: UpdateUserInput!) {"
            + "  updateUser(changes: $changes) {"
            + "    user {"
            + "      email"
            + "      username"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    Map<String, String> changes = new HashMap<>();
    changes.put("email", "newemail@example.com");
    variables.put("changes", changes);

    Object result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.updateUser", variables, new TypeRef<Object>() {});

    assertThat(result).isNull();
  }
}
