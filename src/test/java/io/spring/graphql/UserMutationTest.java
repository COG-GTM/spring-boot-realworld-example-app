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
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
  private io.spring.application.UserQueryService userQueryService;

  @MockBean
  private JwtService jwtService;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User("user@example.com", "testuser", "encodedPassword", "bio", "image");

    AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
        "key", "anonymous", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldCreateUser() {
    User newUser = new User("newuser@example.com", "newuser", "encodedPassword", "", "");
    when(userService.createUser(any())).thenReturn(newUser);
    when(jwtService.toToken(eq(newUser))).thenReturn("jwt-token");

    String mutation = "mutation { createUser(input: {email: \"newuser@example.com\", username: \"newuser\", password: \"password123\"}) { ... on UserPayload { user { email username token } } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createUser.user");

    assertThat(result).isNotNull();
    assertThat(result.get("email")).isEqualTo("newuser@example.com");
    assertThat(result.get("username")).isEqualTo("newuser");
    assertThat(result.get("token")).isEqualTo("jwt-token");
  }

  @Test
  void shouldLoginSuccessfully() {
    when(userRepository.findByEmail(eq("user@example.com"))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("password123"), eq("encodedPassword"))).thenReturn(true);
    when(jwtService.toToken(eq(user))).thenReturn("jwt-token");

    String mutation = "mutation { login(email: \"user@example.com\", password: \"password123\") { user { email username token } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login.user");

    assertThat(result).isNotNull();
    assertThat(result.get("email")).isEqualTo("user@example.com");
    assertThat(result.get("username")).isEqualTo("testuser");
    assertThat(result.get("token")).isEqualTo("jwt-token");
  }

  @Test
  void shouldFailLoginWithWrongPassword() {
    when(userRepository.findByEmail(eq("user@example.com"))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("wrongpassword"), eq("encodedPassword"))).thenReturn(false);

    String mutation = "mutation { login(email: \"user@example.com\", password: \"wrongpassword\") { user { email } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldFailLoginWithNonExistentUser() {
    when(userRepository.findByEmail(eq("nonexistent@example.com"))).thenReturn(Optional.empty());

    String mutation = "mutation { login(email: \"nonexistent@example.com\", password: \"password123\") { user { email } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldUpdateUser() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, Arrays.asList()));

    String mutation = "mutation { updateUser(changes: {bio: \"Updated bio\", image: \"new-image.jpg\"}) { user { email username } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateUser.user");

    assertThat(result).isNotNull();
    assertThat(result.get("email")).isEqualTo("user@example.com");
    assertThat(result.get("username")).isEqualTo("testuser");
  }

  @Test
  void shouldReturnNullWhenUpdatingUserWithoutAuth() {
    String mutation = "mutation { updateUser(changes: {bio: \"Updated bio\"}) { user { email } } }";

    Object result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateUser");

    assertThat(result).isNull();
  }
}
