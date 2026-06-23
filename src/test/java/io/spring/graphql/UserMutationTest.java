package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.InvalidAuthenticationException;
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
import org.springframework.security.crypto.password.PasswordEncoder;

class UserMutationTest {

  private UserRepository userRepository;
  private PasswordEncoder encryptService;
  private UserService userService;
  private UserMutation mutation;
  private User user;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    encryptService = mock(PasswordEncoder.class);
    userService = mock(UserService.class);
    mutation = new UserMutation(userRepository, encryptService, userService);
    user = new User("user@test.com", "user", "encoded", "bio", "image");
  }

  @AfterEach
  void tearDown() {
    GraphQLTestSecurity.clear();
  }

  @Test
  void createUser_success_returns_payload_with_user_context() {
    CreateUserInput input = new CreateUserInput("user@test.com", "user", "123");
    when(userService.createUser(any())).thenReturn(user);

    DataFetcherResult<UserResult> result = mutation.createUser(input);

    assertTrue(result.getData() instanceof UserPayload);
    assertEquals(user, result.getLocalContext());
  }

  @Test
  void createUser_with_constraint_violation_returns_error_data() {
    CreateUserInput input = new CreateUserInput("bad", "user", "123");
    when(userService.createUser(any()))
        .thenThrow(new ConstraintViolationException("invalid", Collections.emptySet()));

    DataFetcherResult<UserResult> result = mutation.createUser(input);

    assertTrue(result.getData() instanceof Error);
    assertEquals("BAD_REQUEST", ((Error) result.getData()).getMessage());
    assertNull(result.getLocalContext());
  }

  @Test
  void login_success_returns_payload() {
    when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("123", "encoded")).thenReturn(true);

    DataFetcherResult<UserPayload> result = mutation.login("123", "user@test.com");

    assertNotNull(result.getData());
    assertEquals(user, result.getLocalContext());
  }

  @Test
  void login_with_wrong_password_throws() {
    when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("wrong", "encoded")).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class, () -> mutation.login("wrong", "user@test.com"));
  }

  @Test
  void login_with_unknown_email_throws() {
    when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

    assertThrows(
        InvalidAuthenticationException.class, () -> mutation.login("123", "ghost@test.com"));
  }

  @Test
  void updateUser_when_authenticated_updates_and_returns_payload() {
    GraphQLTestSecurity.login(user);
    UpdateUserInput input =
        new UpdateUserInput("new@test.com", "newname", "newpass", "newimage", "newbio");

    DataFetcherResult<UserPayload> result = mutation.updateUser(input);

    assertNotNull(result.getData());
    assertEquals(user, result.getLocalContext());
    verify(userService).updateUser(any());
  }

  @Test
  void updateUser_when_anonymous_returns_null() {
    GraphQLTestSecurity.anonymous();
    UpdateUserInput input =
        new UpdateUserInput("new@test.com", "newname", "newpass", "newimage", "newbio");

    assertNull(mutation.updateUser(input));
  }
}
