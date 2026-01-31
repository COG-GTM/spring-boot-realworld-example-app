package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.user.UserService;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
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

@SpringBootTest
public class UserMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;

  @MockBean private PasswordEncoder passwordEncoder;

  @MockBean private UserService userService;

  @MockBean private JwtService jwtService;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("test@test.com", "testuser", "encodedpassword", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_user() {
    when(userService.createUser(any())).thenReturn(user);
    when(jwtService.toToken(eq(user))).thenReturn("test-token");

    String mutation =
        "mutation { createUser(input: { email: \"test@test.com\", username: \"testuser\", password: \"password\" }) { ... on UserPayload { user { email username token } } } }";

    String email =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createUser.user.email");
    String username =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createUser.user.username");
    String token =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createUser.user.token");

    assert email.equals("test@test.com");
    assert username.equals("testuser");
    assert token.equals("test-token");
  }

  @Test
  public void should_login_user() {
    when(userRepository.findByEmail(eq("test@test.com"))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("password"), eq("encodedpassword"))).thenReturn(true);
    when(jwtService.toToken(eq(user))).thenReturn("test-token");

    String mutation =
        "mutation { login(email: \"test@test.com\", password: \"password\") { user { email username token } } }";

    String email = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login.user.email");
    String token = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login.user.token");

    assert email.equals("test@test.com");
    assert token.equals("test-token");
  }

  @Test
  public void should_fail_login_with_wrong_password() {
    when(userRepository.findByEmail(eq("test@test.com"))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("wrongpassword"), eq("encodedpassword"))).thenReturn(false);

    String mutation =
        "mutation { login(email: \"test@test.com\", password: \"wrongpassword\") { user { email } } }";

    try {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login.user.email");
      assert false : "Should have thrown exception";
    } catch (Exception e) {
      assert true;
    }
  }

  @Test
  public void should_fail_login_with_nonexistent_email() {
    when(userRepository.findByEmail(eq("nonexistent@test.com"))).thenReturn(Optional.empty());

    String mutation =
        "mutation { login(email: \"nonexistent@test.com\", password: \"password\") { user { email } } }";

    try {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login.user.email");
      assert false : "Should have thrown exception";
    } catch (Exception e) {
      assert true;
    }
  }

  @Test
  public void should_update_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    String mutation =
        "mutation { updateUser(changes: { bio: \"new bio\" }) { user { email username } } }";

    String email =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateUser.user.email");

    assert email.equals("test@test.com");
  }

  @Test
  public void should_fail_when_updating_without_auth() {
    String mutation =
        "mutation { updateUser(changes: { bio: \"new bio\" }) { user { email } } }";

    try {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateUser.user.email");
      assertThat(false).as("Should have thrown exception").isTrue();
    } catch (Exception e) {
      assertThat(true).isTrue();
    }
  }
}
