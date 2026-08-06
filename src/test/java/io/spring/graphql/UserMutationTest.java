package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.application.ProfileQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import io.spring.application.user.UpdateUserParam;
import io.spring.application.user.UserService;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      UserMutation.class,
      MeDatafetcher.class,
      ProfileDatafetcher.class
    })
public class UserMutationTest extends DgsTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;
  @MockBean private PasswordEncoder encryptService;
  @MockBean private UserService userService;
  @MockBean private UserQueryService userQueryService;
  @MockBean private ProfileQueryService profileQueryService;
  @MockBean private JwtService jwtService;

  private static final Validator VALIDATOR =
      Validation.buildDefaultValidatorFactory().getValidator();

  private User user;

  @BeforeEach
  void setUp() {
    user = TestHelper.userFixture("john");
    authenticateAnonymously();
    when(jwtService.toToken(any(User.class))).thenReturn("jwt-token");
  }

  private static ConstraintViolationException emailViolation() {
    Set<ConstraintViolation<UpdateUserParam>> violations =
        VALIDATOR.validate(UpdateUserParam.builder().email("not-an-email").build());
    return new ConstraintViolationException(violations);
  }

  @Test
  public void should_create_user_and_return_user_payload() {
    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);

    Map<String, Object> created =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { createUser(input: {email: \""
                + user.getEmail()
                + "\", username: \""
                + user.getUsername()
                + "\", password: \"123\"}) { ... on UserPayload { user { email username token } }"
                + " } }",
            "data.createUser.user");

    assertThat(created.get("email")).isEqualTo(user.getEmail());
    assertThat(created.get("username")).isEqualTo(user.getUsername());
    assertThat(created.get("token")).isEqualTo("jwt-token");

    ArgumentCaptor<RegisterParam> captor = ArgumentCaptor.forClass(RegisterParam.class);
    verify(userService).createUser(captor.capture());
    assertThat(captor.getValue().getEmail()).isEqualTo(user.getEmail());
    assertThat(captor.getValue().getPassword()).isEqualTo("123");
  }

  @Test
  public void should_return_error_result_when_registration_is_invalid() {
    when(userService.createUser(any(RegisterParam.class))).thenThrow(emailViolation());

    Map<String, Object> error =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { createUser(input: {email: \"invalid\", username: \"john\", password:"
                + " \"123\"}) { ... on Error { message errors { key value } } } }",
            "data.createUser");

    assertThat(error.get("message")).isEqualTo("BAD_REQUEST");
    List<Map<String, Object>> errors = (List<Map<String, Object>>) error.get("errors");
    assertThat(errors).hasSize(1);
    assertThat(errors.get(0).get("key")).isEqualTo("email");
    assertThat((List<String>) errors.get(0).get("value")).containsExactly("should be an email");
  }

  @Test
  public void should_login_with_valid_credentials() {
    when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("123"), eq(user.getPassword()))).thenReturn(true);

    Map<String, Object> loggedIn =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { login(email: \""
                + user.getEmail()
                + "\", password: \"123\") { user { email username token } } }",
            "data.login.user");

    assertThat(loggedIn.get("email")).isEqualTo(user.getEmail());
    assertThat(loggedIn.get("token")).isEqualTo("jwt-token");
  }

  @Test
  public void should_resolve_profile_of_logged_in_user() {
    when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("123"), eq(user.getPassword()))).thenReturn(true);
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(TestHelper.profileDataFixture(user)));

    String username =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { login(email: \""
                + user.getEmail()
                + "\", password: \"123\") { user { profile { username following } } } }",
            "data.login.user.profile.username");

    assertThat(username).isEqualTo(user.getUsername());
  }

  @Test
  public void should_not_login_with_wrong_password() {
    when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("wrong"), eq(user.getPassword()))).thenReturn(false);

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { login(email: \""
                + user.getEmail()
                + "\", password: \"wrong\") { user { email } } }");

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_not_login_with_unknown_email() {
    when(userRepository.findByEmail(eq("ghost@test.com"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { login(email: \"ghost@test.com\", password: \"123\") { user { email } } }");

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_update_current_user() {
    authenticate(user);

    Map<String, Object> updated =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { updateUser(changes: {email: \"new@test.com\", bio: \"new bio\"}) { user {"
                + " email username token } } }",
            "data.updateUser.user");

    assertThat(updated.get("username")).isEqualTo(user.getUsername());

    ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
    verify(userService).updateUser(captor.capture());
    assertThat(captor.getValue().getTargetUser()).isEqualTo(user);
    assertThat(captor.getValue().getParam().getEmail()).isEqualTo("new@test.com");
    assertThat(captor.getValue().getParam().getBio()).isEqualTo("new bio");
  }

  @Test
  public void should_return_null_when_updating_user_anonymously() {
    Map<String, Object> data =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { updateUser(changes: {bio: \"new bio\"}) { user { email } } }", "data");

    assertThat(data).containsEntry("updateUser", null);
    verify(userService, never()).updateUser(any());
  }
}
