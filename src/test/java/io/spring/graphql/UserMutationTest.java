package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import com.netflix.graphql.types.errors.ErrorType;
import graphql.ExecutionResult;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import io.spring.application.user.UserService;
import io.spring.core.service.JwtService;
import io.spring.core.user.UserRepository;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      GraphQLCustomizeExceptionHandler.class,
      UserMutation.class,
      MeDatafetcher.class
    })
public class UserMutationTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;

  @MockBean private PasswordEncoder encryptService;

  @MockBean private UserService userService;

  @MockBean private io.spring.application.UserQueryService userQueryService;

  @MockBean private JwtService jwtService;

  @Test
  void should_create_user() {
    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);
    when(jwtService.toToken(eq(user))).thenReturn("new-token");

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { createUser(input: {email: \"john@jacob.com\", username: \"johnjacob\","
                + " password: \"123\"}) { ... on UserPayload { user { email username token } } } }");

    assertThat(context.read("$.data.createUser.user.email", String.class))
        .isEqualTo(user.getEmail());
    assertThat(context.read("$.data.createUser.user.username", String.class))
        .isEqualTo(user.getUsername());
    assertThat(context.read("$.data.createUser.user.token", String.class)).isEqualTo("new-token");

    ArgumentCaptor<RegisterParam> captor = ArgumentCaptor.forClass(RegisterParam.class);
    verify(userService).createUser(captor.capture());
    assertThat(captor.getValue().getEmail()).isEqualTo("john@jacob.com");
    assertThat(captor.getValue().getUsername()).isEqualTo("johnjacob");
    assertThat(captor.getValue().getPassword()).isEqualTo("123");
  }

  @Test
  void should_return_error_result_when_registration_is_invalid() {
    when(userService.createUser(any(RegisterParam.class)))
        .thenThrow(constraintViolationException());

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { createUser(input: {email: \"john@jacob.com\", username: \"\", password:"
                + " \"123\"}) { ... on Error { message errors { key value } } } }");

    assertThat(context.read("$.data.createUser.message", String.class)).isEqualTo("BAD_REQUEST");
    assertThat(context.read("$.data.createUser.errors[0].key", String.class)).isEqualTo("username");
    assertThat(context.read("$.data.createUser.errors[0].value", List.class))
        .containsExactly("can't be empty");
  }

  @Test
  void should_login_with_valid_credentials() {
    when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("123"), eq(user.getPassword()))).thenReturn(true);
    when(jwtService.toToken(eq(user))).thenReturn("login-token");

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { login(email: \"john@jacob.com\", password: \"123\") { user { email"
                + " username token } } }");

    assertThat(context.read("$.data.login.user.username", String.class))
        .isEqualTo(user.getUsername());
    assertThat(context.read("$.data.login.user.token", String.class)).isEqualTo("login-token");
  }

  @Test
  void should_fail_login_with_wrong_password() {
    when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("wrong"), eq(user.getPassword()))).thenReturn(false);

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { login(email: \"john@jacob.com\", password: \"wrong\") { user { email } }"
                + " }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).isEqualTo("invalid email or password");
    assertThat(result.getErrors().get(0).getExtensions())
        .containsEntry("errorType", ErrorType.UNAUTHENTICATED.name());
  }

  @Test
  void should_fail_login_with_unknown_email() {
    when(userRepository.findByEmail(eq("ghost@jacob.com"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { login(email: \"ghost@jacob.com\", password: \"123\") { user { email } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getExtensions())
        .containsEntry("errorType", ErrorType.UNAUTHENTICATED.name());
  }

  @Test
  void should_update_current_user() {
    when(jwtService.toToken(eq(user))).thenReturn("updated-token");

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { updateUser(changes: {email: \"jane@jacob.com\", username: \"janejacob\","
                + " bio: \"new bio\", image: \"new-image\", password: \"newpassword\"}) { user {"
                + " email username token } } }");

    assertThat(context.read("$.data.updateUser.user.token", String.class))
        .isEqualTo("updated-token");

    ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
    verify(userService).updateUser(captor.capture());
    assertThat(captor.getValue().getTargetUser()).isEqualTo(user);
    assertThat(captor.getValue().getParam().getEmail()).isEqualTo("jane@jacob.com");
    assertThat(captor.getValue().getParam().getUsername()).isEqualTo("janejacob");
    assertThat(captor.getValue().getParam().getBio()).isEqualTo("new bio");
    assertThat(captor.getValue().getParam().getImage()).isEqualTo("new-image");
    assertThat(captor.getValue().getParam().getPassword()).isEqualTo("newpassword");
  }

  @Test
  void should_not_update_user_for_anonymous_request() {
    logout();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { updateUser(changes: {email: \"jane@jacob.com\"}) { user { email } } }");

    assertThat(result.getErrors()).isEmpty();
    assertThat(result.<Map<String, Object>>getData()).containsEntry("updateUser", null);
    verify(userService, org.mockito.Mockito.never()).updateUser(any());
  }

  private ConstraintViolationException constraintViolationException() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      Set<ConstraintViolation<RegistrationForm>> violations =
          factory.getValidator().validate(new RegistrationForm(""));
      return new ConstraintViolationException(violations);
    }
  }

  private static class RegistrationForm {
    @NotBlank(message = "can't be empty")
    private final String username;

    RegistrationForm(String username) {
      this.username = username;
    }
  }
}
