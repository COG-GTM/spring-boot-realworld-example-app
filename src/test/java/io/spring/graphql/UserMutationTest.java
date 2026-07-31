package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import com.netflix.graphql.types.errors.ErrorType;
import graphql.ExecutionResult;
import io.spring.application.UserQueryService;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import io.spring.application.user.UpdateUserParam;
import io.spring.application.user.UserService;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.HashSet;
import java.util.List;
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
      GraphQLCustomizeExceptionHandler.class,
      UserMutation.class,
      MeDatafetcher.class
    })
class UserMutationTest extends GraphQLTestBase {

  private static final String TOKEN = "jwt-token";

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;
  @MockBean private UserService userService;
  @MockBean private PasswordEncoder passwordEncoder;
  @MockBean private UserQueryService userQueryService;
  @MockBean private JwtService jwtService;

  private User user;

  @BeforeEach
  void setUp() {
    user = userFixture("john");
    when(jwtService.toToken(eq(user))).thenReturn(TOKEN);
  }

  @Test
  void should_create_user() {
    anonymous();
    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "mutation { createUser(input: {email: \"%s\", username: \"%s\", password: \"123\"}) { ... on UserPayload { user { email username token } } } }",
                user.getEmail(), user.getUsername()));

    ArgumentCaptor<RegisterParam> captor = ArgumentCaptor.forClass(RegisterParam.class);
    verify(userService).createUser(captor.capture());
    assertThat(captor.getValue().getEmail()).isEqualTo(user.getEmail());
    assertThat(captor.getValue().getPassword()).isEqualTo("123");
    assertThat(context.read("data.createUser.user.email", String.class)).isEqualTo(user.getEmail());
    assertThat(context.read("data.createUser.user.token", String.class)).isEqualTo(TOKEN);
  }

  @Test
  void should_return_error_payload_when_registration_is_invalid() {
    anonymous();
    when(userService.createUser(any(RegisterParam.class))).thenThrow(blankRegistrationViolation());

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { createUser(input: {email: \"\", username: \"\", password: \"\"}) { ... on Error { message errors { key value } } } }");

    assertThat(context.read("data.createUser.message", String.class)).isEqualTo("BAD_REQUEST");
    assertThat(context.<List<String>>read("data.createUser.errors[*].key"))
        .containsExactlyInAnyOrder("email", "password");
    assertThat(context.<List<List<String>>>read("data.createUser.errors[*].value"))
        .allSatisfy(messages -> assertThat(messages).containsExactly("can't be empty"));
  }

  @Test
  void should_login_with_valid_credentials() {
    anonymous();
    when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("123"), eq(user.getPassword()))).thenReturn(true);

    String token =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "mutation { login(email: \"%s\", password: \"123\") { user { email token } } }",
                user.getEmail()),
            "data.login.user.token");

    assertThat(token).isEqualTo(TOKEN);
  }

  @Test
  void should_return_error_when_password_does_not_match() {
    anonymous();
    when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(any(), any())).thenReturn(false);

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { login(email: \"%s\", password: \"wrong\") { user { token } } }",
                user.getEmail()));

    assertUnauthenticated(result);
  }

  @Test
  void should_return_error_when_login_email_is_unknown() {
    anonymous();
    when(userRepository.findByEmail(eq("ghost@test.com"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { login(email: \"ghost@test.com\", password: \"123\") { user { token } } }");

    assertUnauthenticated(result);
  }

  @Test
  void should_update_current_user() {
    authenticate(user);

    String token =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { updateUser(changes: {email: \"new@test.com\", bio: \"new bio\"}) { user { token } } }",
            "data.updateUser.user.token");

    ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
    verify(userService).updateUser(captor.capture());
    UpdateUserParam param = captor.getValue().getParam();
    assertThat(captor.getValue().getTargetUser()).isEqualTo(user);
    assertThat(param.getEmail()).isEqualTo("new@test.com");
    assertThat(param.getBio()).isEqualTo("new bio");
    assertThat(token).isEqualTo(TOKEN);
  }

  @Test
  void should_not_update_user_for_anonymous_user() {
    anonymous();

    Object updateUser =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { updateUser(changes: {bio: \"new bio\"}) { user { token } } }",
            "data.updateUser");

    assertThat(updateUser).isNull();
    verify(userService, never()).updateUser(any());
  }

  private void assertUnauthenticated(ExecutionResult result) {
    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).isEqualTo("invalid email or password");
    assertThat(result.getErrors().get(0).getExtensions())
        .containsEntry("errorType", ErrorType.UNAUTHENTICATED.name());
  }

  private ConstraintViolationException blankRegistrationViolation() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<RegisterParam>> violations = new HashSet<>();
    violations.addAll(validator.validateValue(RegisterParam.class, "email", ""));
    violations.addAll(validator.validateValue(RegisterParam.class, "password", ""));
    return new ConstraintViolationException(violations);
  }
}
