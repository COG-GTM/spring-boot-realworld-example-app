package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.CreateUserInput;
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserMutationTest {

  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private UserService userService;
  private UserMutation userMutation;
  private User user;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    userService = mock(UserService.class);
    userMutation = new UserMutation(userRepository, passwordEncoder, userService);

    user = new User("test@test.com", "testuser", "encoded", "bio", "image");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_user() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("new@test.com")
            .username("newuser")
            .password("password")
            .build();

    User newUser = new User("new@test.com", "newuser", "encoded", "", "");
    when(userService.createUser(any())).thenReturn(newUser);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);
    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(newUser, result.getLocalContext());
  }

  @Test
  public void should_login_with_valid_credentials() {
    when(userRepository.findByEmail(eq("test@test.com"))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("password"), eq("encoded"))).thenReturn(true);

    DataFetcherResult<UserPayload> result =
        userMutation.login("password", "test@test.com");
    assertNotNull(result);
    assertEquals(user, result.getLocalContext());
  }

  @Test
  public void should_throw_when_login_with_wrong_password() {
    when(userRepository.findByEmail(eq("test@test.com"))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("wrong"), eq("encoded"))).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("wrong", "test@test.com"));
  }

  @Test
  public void should_throw_when_login_with_nonexistent_email() {
    when(userRepository.findByEmail(eq("noone@test.com"))).thenReturn(Optional.empty());

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("password", "noone@test.com"));
  }

  @Test
  public void should_update_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(user, null));

    UpdateUserInput updateInput =
        UpdateUserInput.newBuilder()
            .username("updated")
            .email("updated@test.com")
            .bio("new bio")
            .build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(updateInput);
    assertNotNull(result);
    assertEquals(user, result.getLocalContext());
    verify(userService).updateUser(any());
  }

  @Test
  public void should_return_null_when_anonymous_updates_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new org.springframework.security.authentication.AnonymousAuthenticationToken(
                "key",
                "anonymous",
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList(
                    "ROLE_ANONYMOUS")));

    UpdateUserInput updateInput =
        UpdateUserInput.newBuilder().username("updated").build();
    DataFetcherResult<UserPayload> result = userMutation.updateUser(updateInput);
    assertNull(result);
  }
}
