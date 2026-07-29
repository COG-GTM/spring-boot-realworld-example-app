package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.CreateUserInput;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserMutationTest extends GraphqlTestBase {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder encryptService;
  @Mock private UserService userService;

  @InjectMocks private UserMutation userMutation;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User("john@jacob.com", "johnjacob", "encoded-123", "", "");
  }

  @Test
  void should_create_user() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("john@jacob.com")
            .username("johnjacob")
            .password("123")
            .build();
    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertThat(result.getData()).isInstanceOf(UserPayload.class);
    assertThat((User) result.getLocalContext()).isEqualTo(user);
    ArgumentCaptor<RegisterParam> captor = ArgumentCaptor.forClass(RegisterParam.class);
    verify(userService).createUser(captor.capture());
    assertThat(captor.getValue().getEmail()).isEqualTo("john@jacob.com");
    assertThat(captor.getValue().getUsername()).isEqualTo("johnjacob");
    assertThat(captor.getValue().getPassword()).isEqualTo("123");
  }

  @Test
  void should_return_errors_as_data_when_create_user_is_invalid() {
    when(userService.createUser(any(RegisterParam.class)))
        .thenThrow(ConstraintViolationFixture.beanViolations());

    DataFetcherResult<UserResult> result =
        userMutation.createUser(CreateUserInput.newBuilder().build());

    assertThat(result.getData()).isInstanceOf(Error.class);
    Error error = (Error) result.getData();
    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).extracting("key").contains("username", "email");
    assertThat(result.getLocalContext()).isNull();
  }

  @Test
  void should_login_with_valid_credentials() {
    when(userRepository.findByEmail("john@jacob.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("123", user.getPassword())).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("123", "john@jacob.com");

    assertThat(result.getData()).isInstanceOf(UserPayload.class);
    assertThat((User) result.getLocalContext()).isEqualTo(user);
  }

  @Test
  void should_not_login_with_wrong_password() {
    when(userRepository.findByEmail("john@jacob.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("wrong", user.getPassword())).thenReturn(false);

    assertThatExceptionOfType(InvalidAuthenticationException.class)
        .isThrownBy(() -> userMutation.login("wrong", "john@jacob.com"));
  }

  @Test
  void should_not_login_with_unknown_email() {
    when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

    assertThatExceptionOfType(InvalidAuthenticationException.class)
        .isThrownBy(() -> userMutation.login("123", "unknown@test.com"));
  }

  @Test
  void should_update_current_user() {
    login(user);
    UpdateUserInput changes =
        UpdateUserInput.newBuilder()
            .email("new@test.com")
            .username("newname")
            .bio("new bio")
            .password("new password")
            .image("new image")
            .build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(changes);

    assertThat(result.getData()).isInstanceOf(UserPayload.class);
    assertThat((User) result.getLocalContext()).isEqualTo(user);
    ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
    verify(userService).updateUser(captor.capture());
    assertThat(captor.getValue().getTargetUser()).isEqualTo(user);
    assertThat(captor.getValue().getParam().getEmail()).isEqualTo("new@test.com");
    assertThat(captor.getValue().getParam().getUsername()).isEqualTo("newname");
    assertThat(captor.getValue().getParam().getBio()).isEqualTo("new bio");
    assertThat(captor.getValue().getParam().getImage()).isEqualTo("new image");
    assertThat(captor.getValue().getParam().getPassword()).isEqualTo("new password");
  }

  @Test
  void should_not_update_user_when_anonymous() {
    logout();

    assertThat(userMutation.updateUser(UpdateUserInput.newBuilder().build())).isNull();
    verify(userService, never()).updateUser(any(UpdateUserCommand.class));
  }
}
