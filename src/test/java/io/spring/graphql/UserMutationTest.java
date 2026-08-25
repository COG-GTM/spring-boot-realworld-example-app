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
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder encryptService;
  @Mock private UserService userService;

  private UserMutation userMutation;
  private User user;

  static class Bean {
    @NotBlank(message = "can't be empty")
    private String username;
  }

  @BeforeEach
  public void setUp() {
    userMutation = new UserMutation(userRepository, encryptService, userService);
    user = new User("a@test.com", "aisensiy", "encoded", "bio", "image");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_user() {
    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);

    DataFetcherResult<UserResult> result =
        userMutation.createUser(new CreateUserInput("a@test.com", "aisensiy", "123"));

    ArgumentCaptor<RegisterParam> captor = ArgumentCaptor.forClass(RegisterParam.class);
    verify(userService).createUser(captor.capture());
    assertThat(captor.getValue().getEmail()).isEqualTo("a@test.com");
    assertThat(captor.getValue().getUsername()).isEqualTo("aisensiy");
    assertThat(captor.getValue().getPassword()).isEqualTo("123");
    assertThat(result.getData()).isInstanceOf(UserPayload.class);
    assertThat(result.getLocalContext()).isEqualTo(user);
  }

  @Test
  public void should_return_errors_as_data_when_creating_invalid_user() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<Bean>> violations = validator.validate(new Bean());
    when(userService.createUser(any(RegisterParam.class)))
        .thenThrow(new ConstraintViolationException(violations));

    DataFetcherResult<UserResult> result =
        userMutation.createUser(new CreateUserInput("a@test.com", "", "123"));

    assertThat(result.getData()).isInstanceOf(Error.class);
    Error error = (Error) result.getData();
    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).hasSize(1);
    assertThat(error.getErrors().get(0).getKey()).isEqualTo("username");
    assertThat(error.getErrors().get(0).getValue()).containsExactly("can't be empty");
    assertThat(result.getLocalContext()).isNull();
  }

  @Test
  public void should_login_with_valid_credentials() {
    when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("123", "encoded")).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("123", "a@test.com");

    assertThat(result.getData()).isNotNull();
    assertThat(result.getLocalContext()).isEqualTo(user);
  }

  @Test
  public void should_fail_login_with_wrong_password() {
    when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("wrong", "encoded")).thenReturn(false);

    assertThatExceptionOfType(InvalidAuthenticationException.class)
        .isThrownBy(() -> userMutation.login("wrong", "a@test.com"));
  }

  @Test
  public void should_fail_login_with_unknown_email() {
    when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

    assertThatExceptionOfType(InvalidAuthenticationException.class)
        .isThrownBy(() -> userMutation.login("123", "unknown@test.com"));
  }

  @Test
  public void should_update_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    UpdateUserInput input =
        UpdateUserInput.newBuilder()
            .email("new@test.com")
            .username("newname")
            .password("newpassword")
            .bio("new bio")
            .image("new image")
            .build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
    verify(userService).updateUser(captor.capture());
    assertThat(captor.getValue().getTargetUser()).isEqualTo(user);
    assertThat(captor.getValue().getParam().getEmail()).isEqualTo("new@test.com");
    assertThat(captor.getValue().getParam().getUsername()).isEqualTo("newname");
    assertThat(captor.getValue().getParam().getBio()).isEqualTo("new bio");
    assertThat(captor.getValue().getParam().getImage()).isEqualTo("new image");
    assertThat(result.getData()).isNotNull();
    assertThat(result.getLocalContext()).isEqualTo(user);
  }

  @Test
  public void should_not_update_user_for_anonymous_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertThat(userMutation.updateUser(UpdateUserInput.newBuilder().build())).isNull();
    verify(userService, never()).updateUser(any());
  }

  @Test
  public void should_not_update_user_when_principal_is_null() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(null, null));

    assertThat(userMutation.updateUser(UpdateUserInput.newBuilder().build())).isNull();
    verify(userService, never()).updateUser(any());
  }
}
