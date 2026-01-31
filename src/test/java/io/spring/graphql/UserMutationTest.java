package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.user.UserService;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {DgsAutoConfiguration.class, UserMutation.class, MeDatafetcher.class})
@Import({ProfileDatafetcher.class})
@ActiveProfiles("test")
public class UserMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;

  @MockBean private PasswordEncoder passwordEncoder;

  @MockBean private UserService userService;

  @MockBean private JwtService jwtService;

  @MockBean private io.spring.application.UserQueryService userQueryService;

  @MockBean private io.spring.application.ProfileQueryService profileQueryService;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User("test@test.com", "testuser", "encodedPassword", "bio", "image");
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldCreateUser() {
    when(userService.createUser(any())).thenReturn(user);
    when(jwtService.toToken(eq(user))).thenReturn("test-token");

    String mutation = "mutation { createUser(input: { email: \"test@test.com\", username: \"testuser\", password: \"password123\" }) { ... on UserPayload { user { email username token } } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createUser.user");

    assertThat(result.get("email")).isEqualTo("test@test.com");
    assertThat(result.get("username")).isEqualTo("testuser");
    assertThat(result.get("token")).isEqualTo("test-token");
  }

  @Test
  void shouldLoginSuccessfully() {
    when(userRepository.findByEmail(eq("test@test.com"))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("password123"), eq("encodedPassword"))).thenReturn(true);
    when(jwtService.toToken(eq(user))).thenReturn("test-token");

    String mutation = "mutation { login(email: \"test@test.com\", password: \"password123\") { user { email username token } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login.user");

    assertThat(result.get("email")).isEqualTo("test@test.com");
    assertThat(result.get("username")).isEqualTo("testuser");
    assertThat(result.get("token")).isEqualTo("test-token");
  }

  @Test
  void shouldReturnErrorForInvalidLogin() {
    when(userRepository.findByEmail(eq("test@test.com"))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("wrongpassword"), eq("encodedPassword"))).thenReturn(false);

    String mutation = "mutation { login(email: \"test@test.com\", password: \"wrongpassword\") { user { email } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldReturnErrorForNonExistentUser() {
    when(userRepository.findByEmail(eq("nonexistent@test.com"))).thenReturn(Optional.empty());

    String mutation = "mutation { login(email: \"nonexistent@test.com\", password: \"password123\") { user { email } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldUpdateUser() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, null));

    String mutation = "mutation { updateUser(changes: { email: \"newemail@test.com\", bio: \"new bio\" }) { user { email username } } }";

    when(jwtService.toToken(eq(user))).thenReturn("test-token");

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateUser.user");

    assertThat(result.get("email")).isEqualTo("test@test.com");
    assertThat(result.get("username")).isEqualTo("testuser");
  }

  @Test
  void shouldReturnNullWhenUpdatingUserWithoutAuth() {
    SecurityContextHolder.clearContext();

    String mutation = "mutation { updateUser(changes: { email: \"newemail@test.com\" }) { user { email } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getData().toString()).contains("updateUser=null");
  }
}
