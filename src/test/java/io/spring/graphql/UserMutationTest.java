package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
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
public class UserMutationTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserService userService;

  @MockBean private PasswordEncoder passwordEncoder;

  @Test
  public void should_create_user_successfully() {
    String newEmail = "newuser@test.com";
    String newUsername = "newuser";
    String password = "password123";

    User newUser = new User(newEmail, newUsername, password, "", defaultAvatar);
    when(userService.createUser(any())).thenReturn(newUser);
    when(jwtService.toToken(any())).thenReturn("new-token");

    String mutation =
        "mutation { createUser(input: {email: \""
            + newEmail
            + "\", username: \""
            + newUsername
            + "\", password: \""
            + password
            + "\"}) { ... on UserPayload { user { email username token } } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createUser");

    assertThat(result).isNotNull();
  }

  @Test
  public void should_login_successfully() {
    String password = "123";
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq(password), any())).thenReturn(true);
    when(jwtService.toToken(any())).thenReturn(token);

    String mutation =
        "mutation { login(email: \"" + email + "\", password: \"" + password + "\") { user { email username token } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login");

    assertThat(result).isNotNull();
  }

  @Test
  public void should_fail_login_with_wrong_password() {
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(any(), any())).thenReturn(false);

    String mutation =
        "mutation { login(email: \"" + email + "\", password: \"wrongpassword\") { user { email username token } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_fail_login_with_nonexistent_email() {
    when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

    String mutation =
        "mutation { login(email: \"nonexistent@test.com\", password: \"password\") { user { email username token } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_update_user_when_authenticated() {
    setAuthenticatedUser(user);
    when(jwtService.toToken(any())).thenReturn(token);

    String mutation =
        "mutation { updateUser(changes: {bio: \"new bio\", image: \"http://newimage.com\"}) { user { email username token } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateUser");

    assertThat(result).isNotNull();
  }

  @Test
  public void should_return_null_when_updating_user_without_authentication() {
    clearAuthentication();

    String mutation =
        "mutation { updateUser(changes: {bio: \"new bio\"}) { user { email username token } } }";

    Object result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateUser");

    assertThat(result).isNull();
  }
}
