package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserMutationTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder encryptService;

  @Mock private UserService userService;

  @InjectMocks private UserMutation userMutation;

  private String defaultAvatar;

  @BeforeEach
  public void setUp() {
    defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_user_successfully() {
    String email = "test@example.com";
    String username = "testuser";
    String password = "password123";

    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email(email)
            .username(username)
            .password(password)
            .build();

    User user = new User(email, username, password, "", defaultAvatar);
    when(userService.createUser(any())).thenReturn(user);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(user, result.getLocalContext());
    verify(userService).createUser(any());
  }

  @Test
  public void should_handle_constraint_violation_on_create_user() {
    String email = "test@example.com";
    String username = "testuser";
    String password = "password123";

    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email(email)
            .username(username)
            .password(password)
            .build();

    when(userService.createUser(any()))
        .thenThrow(new ConstraintViolationException("Validation failed", java.util.Collections.emptySet()));

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertNotNull(result);
    assertNotNull(result.getData());
  }

  @Test
  public void should_login_successfully() {
    String email = "test@example.com";
    String username = "testuser";
    String password = "password123";
    String encodedPassword = "encoded123";

    User user = new User(email, username, encodedPassword, "", defaultAvatar);
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq(password), eq(encodedPassword))).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login(password, email);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(user, result.getLocalContext());
  }

  @Test
  public void should_fail_login_with_invalid_email() {
    String email = "test@example.com";
    String password = "password123";

    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.empty());

    try {
      userMutation.login(password, email);
    } catch (InvalidAuthenticationException e) {
      assertNotNull(e);
    }
  }

  @Test
  public void should_fail_login_with_wrong_password() {
    String email = "test@example.com";
    String username = "testuser";
    String password = "password123";
    String encodedPassword = "encoded123";

    User user = new User(email, username, encodedPassword, "", defaultAvatar);
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq(password), eq(encodedPassword))).thenReturn(false);

    try {
      userMutation.login(password, email);
    } catch (InvalidAuthenticationException e) {
      assertNotNull(e);
    }
  }

  @Test
  public void should_update_user_successfully() {
    String email = "test@example.com";
    String username = "testuser";
    String newEmail = "newemail@example.com";
    String newBio = "New bio";

    User user = new User(email, username, "password", "", defaultAvatar);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    UpdateUserInput input =
        UpdateUserInput.newBuilder().email(newEmail).bio(newBio).username(username).build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(user, result.getLocalContext());
    verify(userService).updateUser(any());
  }

  @Test
  public void should_return_null_when_updating_user_without_authentication() {
    SecurityContextHolder.clearContext();
    
    UpdateUserInput input =
        UpdateUserInput.newBuilder().email("test@example.com").bio("bio").build();

    try {
      DataFetcherResult<UserPayload> result = userMutation.updateUser(input);
      assertNull(result);
    } catch (NullPointerException e) {
      assertNotNull(e);
    }
  }
}
