package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
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
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      UserMutation.class,
      MeDatafetcher.class,
      ProfileDatafetcher.class,
      WebSecurityConfig.class,
      UserQueryService.class,
      BCryptPasswordEncoder.class,
      JacksonCustomizations.class
    })
public class UserMutationTest {

  @Autowired DgsQueryExecutor dgsQueryExecutor;

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
  public void should_create_user_successfully() {
    String email = "john@jacob.com";
    String username = "johnjacob";
    String password = "password123";

    User user = new User(email, username, password, "", defaultAvatar);
    when(userService.createUser(any())).thenReturn(user);
    when(jwtService.toToken(any())).thenReturn("test-token");

    UserData userData = new UserData(user.getId(), email, username, "", defaultAvatar);
    when(userReadService.findById(eq(user.getId()))).thenReturn(userData);

    String query =
        String.format(
            "mutation { createUser(input: { email: \"%s\", username: \"%s\", password: \"%s\" }) { ... on UserPayload { user { email username bio image token } } ... on Error { message errors { key value } } } }",
            email, username, password);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.createUser.user", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("email")).isEqualTo(email);
    assertThat(result.get("username")).isEqualTo(username);
    assertThat(result.get("token")).isEqualTo("test-token");

    verify(userService).createUser(any());
  }

  @Test
  public void should_return_error_for_duplicate_username() {
    String email = "john@jacob.com";
    String username = "johnjacob";
    String password = "password123";

    when(userService.createUser(any())).thenThrow(new ConstraintViolationException("duplicated username", null));

    String query =
        String.format(
            "mutation { createUser(input: { email: \"%s\", username: \"%s\", password: \"%s\" }) { ... on UserPayload { user { email username } } ... on Error { message errors { key value } } } }",
            email, username, password);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.createUser", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("errors")).isNotNull();
  }

  @Test
  public void should_login_successfully() {
    String email = "john@jacob.com";
    String username = "johnjacob";
    String password = "password123";

    User user = new User(email, username, passwordEncoder.encode(password), "", defaultAvatar);
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(jwtService.toToken(any())).thenReturn("test-token");

    UserData userData = new UserData(user.getId(), email, username, "", defaultAvatar);
    when(userReadService.findById(eq(user.getId()))).thenReturn(userData);

    String query =
        String.format(
            "mutation { login(email: \"%s\", password: \"%s\") { user { email username token } } }",
            email, password);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.login.user", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("email")).isEqualTo(email);
    assertThat(result.get("username")).isEqualTo(username);
    assertThat(result.get("token")).isEqualTo("test-token");
  }

  @Test
  public void should_fail_login_with_wrong_password() {
    String email = "john@jacob.com";
    String username = "johnjacob";
    String password = "password123";
    String wrongPassword = "wrongpassword";

    User user = new User(email, username, passwordEncoder.encode(password), "", defaultAvatar);
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));

    String query =
        String.format(
            "mutation { login(email: \"%s\", password: \"%s\") { user { email username token } } }",
            email, wrongPassword);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.login.user", Map.class);
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("InvalidAuthenticationException");
    }
  }

  @Test
  public void should_fail_login_with_nonexistent_user() {
    String email = "nonexistent@example.com";
    String password = "password123";

    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.empty());

    String query =
        String.format(
            "mutation { login(email: \"%s\", password: \"%s\") { user { email username token } } }",
            email, password);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.login.user", Map.class);
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("InvalidAuthenticationException");
    }
  }

  @Test
  @WithMockUser(username = "johnjacob")
  public void should_update_user_successfully() {
    String email = "john@jacob.com";
    String username = "johnjacob";
    String newBio = "I like to code";
    String newImage = "https://example.com/image.jpg";

    User user = new User(email, username, "password", "", defaultAvatar);
    when(userRepository.findByUsername(eq(username))).thenReturn(Optional.of(user));
    when(jwtService.toToken(any())).thenReturn("test-token");

    UserData userData = new UserData(user.getId(), email, username, newBio, newImage);
    when(userReadService.findById(eq(user.getId()))).thenReturn(userData);

    String query =
        String.format(
            "mutation { updateUser(changes: { bio: \"%s\", image: \"%s\" }) { user { email username bio image token } } }",
            newBio, newImage);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.updateUser.user", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("email")).isEqualTo(email);
    assertThat(result.get("username")).isEqualTo(username);
    assertThat(result.get("bio")).isEqualTo(newBio);
    assertThat(result.get("image")).isEqualTo(newImage);

    verify(userService).updateUser(any());
  }
}
