package io.spring.graphql.mutation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
import io.spring.graphql.UserMutation;
import io.spring.graphql.types.CreateUserInput;
import io.spring.graphql.types.Error;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserMutationTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final PasswordEncoder encryptService = mock(PasswordEncoder.class);
  private final UserService userService = mock(UserService.class);
  private final UserMutation mutation =
      new UserMutation(userRepository, encryptService, userService);

  private final User user = new User("jake@jake.jake", "jake", "encoded", "bio", "image");

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  static class Payload {
    @NotBlank(message = "can't be empty")
    private final String username;

    Payload(String username) {
      this.username = username;
    }
  }

  private static ConstraintViolationException violationException() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<Payload>> violations = validator.validate(new Payload(""));
    return new ConstraintViolationException(violations);
  }

  @Test
  void should_create_user() {
    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);

    DataFetcherResult<UserResult> result =
        mutation.createUser(
            CreateUserInput.newBuilder()
                .email("jake@jake.jake")
                .username("jake")
                .password("123")
                .build());

    assertThat(result.getLocalContext()).isSameAs(user);
    assertThat(result.getData()).isInstanceOf(UserPayload.class);
  }

  @Test
  void should_return_error_data_when_creation_violates_constraints() {
    when(userService.createUser(any(RegisterParam.class))).thenThrow(violationException());

    DataFetcherResult<UserResult> result =
        mutation.createUser(
            CreateUserInput.newBuilder().email("").username("").password("").build());

    assertThat(result.getLocalContext()).isNull();
    assertThat(result.getData()).isInstanceOf(Error.class);
    Error error = (Error) result.getData();
    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).hasSize(1);
    assertThat(error.getErrors().get(0).getKey()).isEqualTo("username");
    assertThat(error.getErrors().get(0).getValue()).containsExactly("can't be empty");
  }

  @Test
  void should_login_with_matching_password() {
    when(userRepository.findByEmail("jake@jake.jake")).thenReturn(Optional.of(user));
    when(encryptService.matches("123", "encoded")).thenReturn(true);

    DataFetcherResult<UserPayload> result = mutation.login("123", "jake@jake.jake");

    assertThat(result.getLocalContext()).isSameAs(user);
    assertThat(result.getData()).isNotNull();
  }

  @Test
  void should_reject_login_with_wrong_password() {
    when(userRepository.findByEmail("jake@jake.jake")).thenReturn(Optional.of(user));
    when(encryptService.matches("wrong", "encoded")).thenReturn(false);

    assertThatExceptionOfType(InvalidAuthenticationException.class)
        .isThrownBy(() -> mutation.login("wrong", "jake@jake.jake"));
  }

  @Test
  void should_reject_login_with_unknown_email() {
    when(userRepository.findByEmail("ghost@ghost.com")).thenReturn(Optional.empty());

    assertThatExceptionOfType(InvalidAuthenticationException.class)
        .isThrownBy(() -> mutation.login("123", "ghost@ghost.com"));
  }

  @Test
  void should_update_current_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, null));

    DataFetcherResult<UserPayload> result =
        mutation.updateUser(
            UpdateUserInput.newBuilder()
                .email("new@jake.jake")
                .username("newjake")
                .bio("new bio")
                .password("newpass")
                .image("new image")
                .build());

    ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
    verify(userService).updateUser(captor.capture());
    assertThat(captor.getValue().getTargetUser()).isSameAs(user);
    assertThat(captor.getValue().getParam().getEmail()).isEqualTo("new@jake.jake");
    assertThat(captor.getValue().getParam().getUsername()).isEqualTo("newjake");
    assertThat(captor.getValue().getParam().getBio()).isEqualTo("new bio");
    assertThat(result.getLocalContext()).isSameAs(user);
  }

  @Test
  void should_return_null_when_updating_as_anonymous_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    assertThat(mutation.updateUser(UpdateUserInput.newBuilder().build())).isNull();
    verify(userService, never()).updateUser(any());
  }
}
