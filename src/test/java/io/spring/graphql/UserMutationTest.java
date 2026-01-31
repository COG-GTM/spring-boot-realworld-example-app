package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.user.UserService;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class UserMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;

  @MockBean private PasswordEncoder passwordEncoder;

  @MockBean private UserService userService;

  @MockBean private JwtService jwtService;

  @Test
  public void should_create_user_success() {
    String email = "test@example.com";
    String username = "testuser";
    String password = "password123";

    User user = new User(email, username, password, "", "");
    when(userService.createUser(any())).thenReturn(user);
    when(jwtService.toToken(any())).thenReturn("test-token");

    String mutation =
        "mutation { createUser(input: {email: \""
            + email
            + "\", username: \""
            + username
            + "\", password: \""
            + password
            + "\"}) { ... on UserPayload { user { email username token } } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(
            mutation, "data.createUser");
    assertNotNull(result);
  }

  @Test
  public void should_login_success() {
    String email = "test@example.com";
    String username = "testuser";
    String password = "password123";
    String encodedPassword = "encoded_password";

    User user = new User(email, username, encodedPassword, "", "");
    when(userRepository.findByEmail(eq(email)))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq(password), eq(encodedPassword)))
        .thenReturn(true);
    when(jwtService.toToken(any())).thenReturn("test-token");

    String mutation =
        "mutation { login(email: \""
            + email
            + "\", password: \""
            + password
            + "\") { user { email username token } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login");
    assertNotNull(result);
  }

  @Test
  public void should_fail_login_with_wrong_password() {
    String email = "test@example.com";
    String username = "testuser";
    String password = "password123";
    String encodedPassword = "encoded_password";

    User user = new User(email, username, encodedPassword, "", "");
    when(userRepository.findByEmail(eq(email)))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq(password), eq(encodedPassword)))
        .thenReturn(false);

    String mutation =
        "mutation { login(email: \""
            + email
            + "\", password: \""
            + password
            + "\") { user { email username token } } }";

    assertThrows(
        Exception.class,
        () -> {
          dgsQueryExecutor.executeAndExtractJsonPath(
              mutation, "data.login");
        });
  }

  @Test
  public void should_fail_login_with_nonexistent_email() {
    String email = "nonexistent@example.com";
    String password = "password123";

    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.empty());

    String mutation =
        "mutation { login(email: \""
            + email
            + "\", password: \""
            + password
            + "\") { user { email username token } } }";

    assertThrows(
        Exception.class,
        () -> {
          dgsQueryExecutor.executeAndExtractJsonPath(
              mutation, "data.login");
        });
  }
}
