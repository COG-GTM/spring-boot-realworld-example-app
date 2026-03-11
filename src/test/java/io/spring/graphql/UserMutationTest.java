package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
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

@SpringBootTest(classes = {DgsAutoConfiguration.class, UserMutation.class, MeDatafetcher.class})
public class UserMutationTest {

  @Autowired
  private DgsQueryExecutor dgsQueryExecutor;

  @MockBean
  private UserRepository userRepository;

  @MockBean
  private PasswordEncoder passwordEncoder;

  @MockBean
  private UserService userService;

  @MockBean
  private UserQueryService userQueryService;

  @MockBean
  private JwtService jwtService;

  @Test
  void shouldCreateUser() {
    User user = new User("test@example.com", "testuser", "encodedPassword", "", "");
    when(userService.createUser(any())).thenReturn(user);
    when(jwtService.toToken(any(User.class))).thenReturn("test-token");

    String mutation = "mutation { createUser(input: { email: \"test@example.com\", username: \"testuser\", password: \"password123\" }) { ... on UserPayload { user { email username token } } } }";

    graphql.ExecutionResult executionResult = dgsQueryExecutor.execute(mutation);

    assertThat(executionResult.getErrors()).isEmpty();
    assertThat((Object) executionResult.getData()).isNotNull();
  }

  @Test
  void shouldLoginUser() {
    User user = new User("test@example.com", "testuser", "encodedPassword", "", "");
    when(userRepository.findByEmail(eq("test@example.com"))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("password123"), eq("encodedPassword"))).thenReturn(true);
    when(jwtService.toToken(any(User.class))).thenReturn("test-token");

    String mutation = "mutation { login(email: \"test@example.com\", password: \"password123\") { user { email username token } } }";

    graphql.ExecutionResult executionResult = dgsQueryExecutor.execute(mutation);

    assertThat(executionResult.getErrors()).isEmpty();
    assertThat((Object) executionResult.getData()).isNotNull();
  }

  @Test
  void shouldFailLoginWithWrongPassword() {
    User user = new User("test@example.com", "testuser", "encodedPassword", "", "");
    when(userRepository.findByEmail(eq("test@example.com"))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("wrongpassword"), eq("encodedPassword"))).thenReturn(false);

    String mutation = "mutation { login(email: \"test@example.com\", password: \"wrongpassword\") { user { email username token } } }";

    graphql.ExecutionResult executionResult = dgsQueryExecutor.execute(mutation);

    assertThat(executionResult.getErrors()).isNotEmpty();
  }

  @Test
  void shouldFailLoginWithNonExistentUser() {
    when(userRepository.findByEmail(eq("nonexistent@example.com"))).thenReturn(Optional.empty());

    String mutation = "mutation { login(email: \"nonexistent@example.com\", password: \"password123\") { user { email username token } } }";

    graphql.ExecutionResult executionResult = dgsQueryExecutor.execute(mutation);

    assertThat(executionResult.getErrors()).isNotEmpty();
  }
}
