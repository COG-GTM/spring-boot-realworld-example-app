package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import java.util.Collections;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserMutationTest {

  private UserRepository userRepository;
  private PasswordEncoder encryptService;
  private UserService userService;
  private UserMutation userMutation;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    encryptService = mock(PasswordEncoder.class);
    userService = mock(UserService.class);
    userMutation = new UserMutation(userRepository, encryptService, userService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
  }

  @Test
  void should_create_user_successfully() {
    User user = new User("new@example.com", "newuser", "123", "", "");
    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);

    DataFetcherResult<UserResult> result =
        userMutation.createUser(new CreateUserInput("new@example.com", "newuser", "123"));

    assertNotNull(result);
    assertEquals(user, result.getLocalContext());
    assertNotNull(result.getData());
  }

  @Test
  void should_return_error_data_when_create_user_violates_constraints() {
    when(userService.createUser(any(RegisterParam.class)))
        .thenThrow(new ConstraintViolationException(Collections.emptySet()));

    DataFetcherResult<UserResult> result =
        userMutation.createUser(new CreateUserInput("bad", "newuser", "123"));

    assertNotNull(result);
    assertNull(result.getLocalContext());
    assertEquals(Error.class, result.getData().getClass());
  }

  @Test
  void should_login_successfully_with_valid_credentials() {
    User user = new User("user@example.com", "user", "encoded", "", "");
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("plain", "encoded")).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("plain", "user@example.com");

    assertEquals(user, result.getLocalContext());
    assertNotNull(result.getData());
  }

  @Test
  void should_throw_when_login_with_wrong_password() {
    User user = new User("user@example.com", "user", "encoded", "", "");
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("wrong", "encoded")).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class, () -> userMutation.login("wrong", "user@example.com"));
  }

  @Test
  void should_throw_when_login_with_unknown_email() {
    when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("plain", "missing@example.com"));
  }

  @Test
  void should_update_user_when_authenticated() {
    User user = new User("user@example.com", "user", "encoded", "", "");
    authenticate(user);
    UpdateUserInput input =
        UpdateUserInput.newBuilder()
            .email("updated@example.com")
            .username("updated")
            .bio("new bio")
            .password("newpass")
            .image("new-image")
            .build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    assertEquals(user, result.getLocalContext());
    verify(userService).updateUser(any(UpdateUserCommand.class));
  }
}
