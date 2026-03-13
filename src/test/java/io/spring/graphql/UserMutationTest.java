package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserMutationTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder encryptService;

  @Mock private UserService userService;

  @InjectMocks private UserMutation userMutation;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createUser_success() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("test@example.com")
            .username("testuser")
            .password("password123")
            .build();
    User user = new User("test@example.com", "testuser", "encoded", "", "");
    when(userService.createUser(any())).thenReturn(user);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertNotNull(result);
    assertInstanceOf(UserPayload.class, result.getData());
    assertEquals(user, result.getLocalContext());
    verify(userService).createUser(any());
  }

  @Test
  void createUser_constraintViolation() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("test@example.com")
            .username("testuser")
            .password("password123")
            .build();
    ConstraintViolationException cve =
        new ConstraintViolationException("validation failed", java.util.Collections.emptySet());
    when(userService.createUser(any())).thenThrow(cve);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertNull(result.getLocalContext());
  }

  @Test
  void login_success() {
    String email = "test@example.com";
    String password = "password123";
    User user = new User(email, "testuser", "encodedPassword", "", "");
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(encryptService.matches(password, user.getPassword())).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login(password, email);

    assertNotNull(result);
    assertInstanceOf(UserPayload.class, result.getData());
    assertEquals(user, result.getLocalContext());
  }

  @Test
  void login_userNotFound() {
    when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("password", "unknown@example.com"));
  }

  @Test
  void login_wrongPassword() {
    String email = "test@example.com";
    User user = new User(email, "testuser", "encodedPassword", "", "");
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(encryptService.matches("wrongPassword", user.getPassword())).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class, () -> userMutation.login("wrongPassword", email));
  }

  @Test
  void updateUser_authenticated() {
    User currentUser = new User("test@example.com", "testuser", "password", "bio", "image");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(currentUser, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    UpdateUserInput updateInput =
        UpdateUserInput.newBuilder()
            .email("new@example.com")
            .username("newuser")
            .bio("new bio")
            .image("newimage.png")
            .password("newpassword")
            .build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(updateInput);

    assertNotNull(result);
    assertInstanceOf(UserPayload.class, result.getData());
    assertEquals(currentUser, result.getLocalContext());
    verify(userService).updateUser(any());
  }

  @Test
  void updateUser_anonymous() {
    AnonymousAuthenticationToken anonAuth =
        new AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            java.util.Collections.singletonList(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonAuth);

    UpdateUserInput updateInput = UpdateUserInput.newBuilder().username("newuser").build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(updateInput);

    assertNull(result);
    verify(userService, never()).updateUser(any());
  }

  @Test
  void updateUser_nullPrincipal() {
    TestingAuthenticationToken auth = new TestingAuthenticationToken(null, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    UpdateUserInput updateInput = UpdateUserInput.newBuilder().username("newuser").build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(updateInput);

    assertNull(result);
    verify(userService, never()).updateUser(any());
  }

  @Test
  void updateUser_partialUpdate() {
    User currentUser = new User("test@example.com", "testuser", "password", "bio", "image");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(currentUser, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    UpdateUserInput updateInput = UpdateUserInput.newBuilder().bio("updated bio only").build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(updateInput);

    assertNotNull(result);
    assertInstanceOf(UserPayload.class, result.getData());
    verify(userService).updateUser(any());
  }
}
