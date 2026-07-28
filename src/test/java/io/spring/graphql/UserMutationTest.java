package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import io.spring.application.user.UserService;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.CreateUserInput;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserMutationTest extends GraphQLTestBase {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder encryptService;
  @Mock private UserService userService;

  @InjectMocks private UserMutation userMutation;

  @Captor private ArgumentCaptor<UpdateUserCommand> updateUserCommandCaptor;

  @Test
  void should_create_user() {
    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);

    DataFetcherResult<UserResult> result =
        userMutation.createUser(
            CreateUserInput.newBuilder()
                .email(user.getEmail())
                .username(user.getUsername())
                .password("123")
                .build());

    assertThat(result.getData()).isInstanceOf(UserPayload.class);
    assertThat((Object) result.getLocalContext()).isEqualTo(user);
  }

  @Test
  void should_return_errors_as_data_when_validation_fails() {
    when(userService.createUser(any(RegisterParam.class)))
        .thenThrow(new ConstraintViolationException(violations()));

    DataFetcherResult<UserResult> result =
        userMutation.createUser(CreateUserInput.newBuilder().build());

    assertThat(result.getData()).isInstanceOf(Error.class);
    Error error = (Error) result.getData();
    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).hasSize(1);
    assertThat(error.getErrors().get(0).getKey()).isEqualTo("name");
    assertThat(error.getErrors().get(0).getValue()).containsExactly("can't be empty");
  }

  @Test
  void should_login_with_valid_credentials() {
    when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("123"), eq(user.getPassword()))).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("123", user.getEmail());

    assertThat(result.getData()).isNotNull();
    assertThat((Object) result.getLocalContext()).isEqualTo(user);
  }

  @Test
  void should_fail_login_with_wrong_password() {
    when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("wrong"), eq(user.getPassword()))).thenReturn(false);

    assertThatExceptionOfType(InvalidAuthenticationException.class)
        .isThrownBy(() -> userMutation.login("wrong", user.getEmail()));
  }

  @Test
  void should_fail_login_with_unknown_email() {
    when(userRepository.findByEmail(eq("unknown@test.com"))).thenReturn(Optional.empty());

    assertThatExceptionOfType(InvalidAuthenticationException.class)
        .isThrownBy(() -> userMutation.login("123", "unknown@test.com"));
  }

  @Test
  void should_update_current_user() {
    DataFetcherResult<UserPayload> result =
        userMutation.updateUser(
            UpdateUserInput.newBuilder()
                .email("new@test.com")
                .username("newname")
                .bio("new bio")
                .password("newpassword")
                .image("new image")
                .build());

    assertThat((Object) result.getLocalContext()).isEqualTo(user);
    org.mockito.Mockito.verify(userService).updateUser(updateUserCommandCaptor.capture());
    UpdateUserCommand command = updateUserCommandCaptor.getValue();
    assertThat(command.getTargetUser()).isEqualTo(user);
    assertThat(command.getParam().getEmail()).isEqualTo("new@test.com");
    assertThat(command.getParam().getUsername()).isEqualTo("newname");
    assertThat(command.getParam().getBio()).isEqualTo("new bio");
    assertThat(command.getParam().getPassword()).isEqualTo("newpassword");
    assertThat(command.getParam().getImage()).isEqualTo("new image");
  }

  @Test
  void should_return_null_updating_user_when_anonymous() {
    anonymous();

    assertThat(userMutation.updateUser(UpdateUserInput.newBuilder().build())).isNull();
  }

  private Set<ConstraintViolation<?>> violations() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    return new HashSet<>(validator.validate(new Named()));
  }

  private static class Named {
    @NotBlank(message = "can't be empty")
    private String name = "";

    public String getName() {
      return name;
    }
  }
}
