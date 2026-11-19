package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import io.spring.graphql.types.ErrorItem;
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.Collections;
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
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserMutationTest {
  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private UserService userService;
  private UserMutation userMutation;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    userService = mock(UserService.class);
    userMutation = new UserMutation(userRepository, passwordEncoder, userService);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  static class EmailBean {
    @NotBlank(message = "can't be empty")
    private String email;
  }

  private ConstraintViolationException blankEmailViolation() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<EmailBean>> violations = validator.validate(new EmailBean());
    return new ConstraintViolationException(violations);
  }

  @Test
  public void createUser_should_return_user_payload_with_created_user_as_local_context() {
    User created = new User("a@b.com", "alice", "hashed", "", "");
    when(userService.createUser(any())).thenReturn(created);

    DataFetcherResult<UserResult> result =
        userMutation.createUser(
            CreateUserInput.newBuilder()
                .email("a@b.com")
                .username("alice")
                .password("secret")
                .build());

    ArgumentCaptor<RegisterParam> captor = ArgumentCaptor.forClass(RegisterParam.class);
    verify(userService).createUser(captor.capture());
    assertThat(captor.getValue().getEmail()).isEqualTo("a@b.com");
    assertThat(captor.getValue().getUsername()).isEqualTo("alice");
    assertThat(captor.getValue().getPassword()).isEqualTo("secret");
    assertThat(result.getData()).isInstanceOf(UserPayload.class);
    assertThat((Object) result.getLocalContext()).isSameAs(created);
  }

  @Test
  public void createUser_should_return_error_data_on_constraint_violation() {
    when(userService.createUser(any())).thenThrow(blankEmailViolation());

    DataFetcherResult<UserResult> result =
        userMutation.createUser(
            CreateUserInput.newBuilder().email("").username("alice").password("secret").build());

    assertThat(result.getData()).isInstanceOf(Error.class);
    Error error = (Error) result.getData();
    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).hasSize(1);
    ErrorItem item = error.getErrors().get(0);
    assertThat(item.getKey()).isEqualTo("email");
    assertThat(item.getValue()).containsExactly("can't be empty");
    assertThat((Object) result.getLocalContext()).isNull();
  }

  @Test
  public void login_should_return_user_when_password_matches() {
    User user = new User("a@b.com", "alice", "hashed", "", "");
    when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("secret", "a@b.com");

    assertThat(result.getData()).isNotNull();
    assertThat((Object) result.getLocalContext()).isSameAs(user);
  }

  @Test
  public void login_should_reject_wrong_password() {
    User user = new User("a@b.com", "alice", "hashed", "", "");
    when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

    assertThatThrownBy(() -> userMutation.login("wrong", "a@b.com"))
        .isInstanceOf(InvalidAuthenticationException.class)
        .hasMessage("invalid email or password");
  }

  @Test
  public void login_should_reject_unknown_email() {
    when(userRepository.findByEmail("nobody@b.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userMutation.login("secret", "nobody@b.com"))
        .isInstanceOf(InvalidAuthenticationException.class);
    verify(passwordEncoder, never()).matches(any(), any());
  }

  @Test
  public void updateUser_should_update_current_user() {
    User user = new User("a@b.com", "alice", "hashed", "", "");
    authenticate(user);

    DataFetcherResult<UserPayload> result =
        userMutation.updateUser(
            UpdateUserInput.newBuilder()
                .username("alice2")
                .email("a2@b.com")
                .bio("new bio")
                .password("newpw")
                .image("new.png")
                .build());

    ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
    verify(userService).updateUser(captor.capture());
    UpdateUserCommand command = captor.getValue();
    assertThat(command.getTargetUser()).isSameAs(user);
    assertThat(command.getParam().getUsername()).isEqualTo("alice2");
    assertThat(command.getParam().getEmail()).isEqualTo("a2@b.com");
    assertThat(command.getParam().getBio()).isEqualTo("new bio");
    assertThat(command.getParam().getPassword()).isEqualTo("newpw");
    assertThat(command.getParam().getImage()).isEqualTo("new.png");
    assertThat(result.getData()).isNotNull();
    assertThat((Object) result.getLocalContext()).isSameAs(user);
  }

  @Test
  public void updateUser_should_return_null_for_anonymous_user() {
    anonymous();

    assertThat(userMutation.updateUser(UpdateUserInput.newBuilder().bio("x").build())).isNull();
    verify(userService, never()).updateUser(any());
  }

  @Test
  public void updateUser_should_return_null_when_principal_is_null() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(null, null, Collections.emptyList()));

    assertThat(userMutation.updateUser(UpdateUserInput.newBuilder().bio("x").build())).isNull();
    verify(userService, never()).updateUser(any());
  }
}
