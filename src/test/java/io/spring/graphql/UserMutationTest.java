package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
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
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      UserMutation.class,
      MeDatafetcher.class
    })
@TestPropertySource(properties = "dgs.graphql.schema-locations=classpath*:schema/**/*.graphqls")
public class UserMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;

  @MockBean private PasswordEncoder passwordEncoder;

  @MockBean private UserService userService;

  @MockBean private JwtService jwtService;

  @MockBean private UserQueryService userQueryService;

  @MockBean private UserReadService userReadService;

  private String defaultAvatar;

  @BeforeEach
  public void setUp() {
    defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";
  }

  @Test
  public void should_create_user_successfully() {
    String email = "john@jacob.com";
    String username = "johnjacob";
    String password = "password123";

    User user = new User(email, username, password, "", defaultAvatar);
    when(userService.createUser(any())).thenReturn(user);
    when(jwtService.toToken(any())).thenReturn("test-token");

    String mutation =
        "mutation CreateUser($input: CreateUserInput!) { "
            + "  createUser(input: $input) { "
            + "    ... on UserPayload { "
            + "      user { "
            + "        email "
            + "        username "
            + "        token "
            + "      } "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    Map<String, Object> input = new HashMap<>();
    input.put("email", email);
    input.put("username", username);
    input.put("password", password);
    variables.put("input", input);

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> createUser = (Map<String, Object>) data.get("createUser");
    assertNotNull(createUser);
    Map<String, Object> userResult = (Map<String, Object>) createUser.get("user");
    assertNotNull(userResult);
    assertEquals(email, userResult.get("email"));
    assertEquals(username, userResult.get("username"));
    assertEquals("test-token", userResult.get("token"));

    verify(userService).createUser(any());
  }

  @Test
  public void should_return_error_for_duplicate_username() {
    String email = "john@jacob.com";
    String username = "johnjacob";
    String password = "password123";

    ConstraintViolationException exception =
        new ConstraintViolationException("duplicated username", null);
    when(userService.createUser(any())).thenThrow(exception);

    String mutation =
        "mutation CreateUser($input: CreateUserInput!) { "
            + "  createUser(input: $input) { "
            + "    ... on UserPayload { "
            + "      user { "
            + "        email "
            + "        username "
            + "      } "
            + "    } "
            + "    ... on Error { "
            + "      message "
            + "      errors { "
            + "        key "
            + "        value "
            + "      } "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    Map<String, Object> input = new HashMap<>();
    input.put("email", email);
    input.put("username", username);
    input.put("password", password);
    variables.put("input", input);

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty(), () -> "Errors: " + result.getErrors());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> createUser = (Map<String, Object>) data.get("createUser");
    assertNotNull(createUser);
    assertNotNull(createUser.get("message"));
  }

  @Test
  public void should_login_successfully() {
    String email = "john@jacob.com";
    String username = "johnjacob";
    String password = "password123";

    User user = new User(email, username, "encoded-password", "", defaultAvatar);
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq(password), eq("encoded-password"))).thenReturn(true);
    when(jwtService.toToken(any())).thenReturn("test-token");

    String mutation =
        "mutation Login($email: String!, $password: String!) { "
            + "  login(email: $email, password: $password) { "
            + "    user { "
            + "      email "
            + "      username "
            + "      token "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("email", email);
    variables.put("password", password);

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> login = (Map<String, Object>) data.get("login");
    assertNotNull(login);
    Map<String, Object> userResult = (Map<String, Object>) login.get("user");
    assertNotNull(userResult);
    assertEquals(email, userResult.get("email"));
    assertEquals(username, userResult.get("username"));
    assertEquals("test-token", userResult.get("token"));
  }

  @Test
  public void should_fail_login_with_wrong_password() {
    String email = "john@jacob.com";
    String username = "johnjacob";
    String password = "password123";
    String wrongPassword = "wrongpassword";

    User user = new User(email, username, "encoded-password", "", defaultAvatar);
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq(wrongPassword), eq("encoded-password"))).thenReturn(false);

    String mutation =
        "mutation Login($email: String!, $password: String!) { "
            + "  login(email: $email, password: $password) { "
            + "    user { "
            + "      email "
            + "      username "
            + "      token "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("email", email);
    variables.put("password", wrongPassword);

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_fail_login_with_nonexistent_email() {
    String email = "nonexistent@example.com";
    String password = "password123";

    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.empty());

    String mutation =
        "mutation Login($email: String!, $password: String!) { "
            + "  login(email: $email, password: $password) { "
            + "    user { "
            + "      email "
            + "      username "
            + "      token "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("email", email);
    variables.put("password", password);

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }
}
