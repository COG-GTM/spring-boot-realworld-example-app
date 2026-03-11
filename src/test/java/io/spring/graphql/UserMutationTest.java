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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(classes = {DgsAutoConfiguration.class, UserMutation.class, MeDatafetcher.class})
public class UserMutationTest extends TestWithCurrentUser {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserService userService;

  @MockBean private PasswordEncoder passwordEncoder;

  @MockBean private UserQueryService userQueryService;

  @Test
  public void should_create_user_success() {
    String newEmail = "newuser@test.com";
    String newUsername = "newuser";
    String password = "password123";

    User newUser = new User(newEmail, newUsername, password, "", defaultAvatar);
    UserData newUserData = new UserData(newUser.getId(), newEmail, newUsername, "", defaultAvatar);

    when(userService.createUser(any())).thenReturn(newUser);
    when(jwtService.toToken(any(User.class))).thenReturn("new-token");
    when(userQueryService.findById(eq(newUser.getId()))).thenReturn(Optional.of(newUserData));

    String mutation =
        "mutation { createUser(input: { email: \""
            + newEmail
            + "\", username: \""
            + newUsername
            + "\", password: \""
            + password
            + "\" }) { ... on UserPayload { user { email username token } } } }";

    String resultEmail =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createUser.user.email");
    String resultUsername =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createUser.user.username");
    String resultToken =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createUser.user.token");

    assertThat(resultEmail).isEqualTo(newEmail);
    assertThat(resultUsername).isEqualTo(newUsername);
    assertThat(resultToken).isEqualTo("new-token");
  }

  @Test
  public void should_login_success() {
    String password = "password123";
    String encodedPassword = "encoded-password";

    User loginUser = new User(email, username, encodedPassword, "", defaultAvatar);

    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(loginUser));
    when(passwordEncoder.matches(eq(password), eq(encodedPassword))).thenReturn(true);
    when(jwtService.toToken(any(User.class))).thenReturn("login-token");

    String mutation =
        "mutation { login(email: \"" + email + "\", password: \"" + password + "\") { user { email username token } } }";

    String resultEmail =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login.user.email");
    String resultUsername =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login.user.username");
    String resultToken =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login.user.token");

    assertThat(resultEmail).isEqualTo(email);
    assertThat(resultUsername).isEqualTo(username);
    assertThat(resultToken).isEqualTo("login-token");
  }

  @Test
  public void should_fail_login_with_wrong_password() {
    String password = "wrongpassword";
    String encodedPassword = "encoded-password";

    User loginUser = new User(email, username, encodedPassword, "", defaultAvatar);

    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(loginUser));
    when(passwordEncoder.matches(eq(password), eq(encodedPassword))).thenReturn(false);

    String mutation =
        "mutation { login(email: \"" + email + "\", password: \"" + password + "\") { user { email } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_fail_login_with_nonexistent_email() {
    when(userRepository.findByEmail(eq("nonexistent@test.com"))).thenReturn(Optional.empty());

    String mutation =
        "mutation { login(email: \"nonexistent@test.com\", password: \"password\") { user { email } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_update_user_success() {
    setAuthenticatedUser(user);

    String newBio = "Updated bio";
    String newImage = "https://example.com/new-image.jpg";

    when(jwtService.toToken(any(User.class))).thenReturn("updated-token");

    String mutation =
        "mutation { updateUser(changes: { bio: \""
            + newBio
            + "\", image: \""
            + newImage
            + "\" }) { user { email username token } } }";

    String resultEmail =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateUser.user.email");
    String resultUsername =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateUser.user.username");

    assertThat(resultEmail).isEqualTo(email);
    assertThat(resultUsername).isEqualTo(username);
  }
}
