package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.Collections;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserMutationTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private UserService userService;

  @InjectMocks private UserMutation userMutation;

  private User testUser;

  @BeforeEach
  public void setUp() {
    testUser = new User("test@example.com", "testuser", "encodedPassword", "bio", "image.jpg");
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_user_successfully() {
    CreateUserInput input = CreateUserInput.newBuilder()
        .email("newuser@test.com")
        .username("newuser")
        .password("password123")
        .build();

    User createdUser = new User("newuser@test.com", "newuser", "encodedPassword", "", "default.jpg");
    when(userService.createUser(any(RegisterParam.class))).thenReturn(createdUser);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.getData());
    Assertions.assertEquals(createdUser, result.getLocalContext());
    verify(userService).createUser(any(RegisterParam.class));
  }

  @Test
  public void should_handle_constraint_violation_on_create_user() {
    CreateUserInput input = CreateUserInput.newBuilder()
        .email("invalid@test.com")
        .username("duplicate")
        .password("pass")
        .build();

    when(userService.createUser(any(RegisterParam.class)))
        .thenThrow(new ConstraintViolationException("Validation failed", Collections.emptySet()));

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.getData());
  }

  @Test
  public void should_login_successfully_with_valid_credentials() {
    String email = "test@example.com";
    String password = "password123";

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login(password, email);

    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.getData());
    Assertions.assertEquals(testUser, result.getLocalContext());
  }

  @Test
  public void should_throw_exception_on_login_with_invalid_email() {
    String email = "nonexistent@test.com";
    String password = "password123";

    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    Assertions.assertThrows(InvalidAuthenticationException.class, () -> {
      userMutation.login(password, email);
    });
  }

  @Test
  public void should_throw_exception_on_login_with_invalid_password() {
    String email = "test@example.com";
    String password = "wrongpassword";

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(false);

    Assertions.assertThrows(InvalidAuthenticationException.class, () -> {
      userMutation.login(password, email);
    });
  }

  @Test
  public void should_update_user_successfully() {
    UpdateUserInput input = UpdateUserInput.newBuilder()
        .username("updateduser")
        .email("updated@test.com")
        .bio("Updated bio")
        .password("newpassword")
        .image("newimage.jpg")
        .build();

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(testUser, null, Collections.emptyList()));

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.getData());
    Assertions.assertEquals(testUser, result.getLocalContext());
    verify(userService).updateUser(any(UpdateUserCommand.class));
  }

  @Test
  public void should_return_null_when_updating_without_authentication() {
    UpdateUserInput input = UpdateUserInput.newBuilder()
        .username("updateduser")
        .email("updated@test.com")
        .build();

    SecurityContextHolder.getContext().setAuthentication(
        new AnonymousAuthenticationToken("key", "anonymous", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    Assertions.assertNull(result);
  }

  @Test
  public void should_update_user_email_only() {
    UpdateUserInput input = UpdateUserInput.newBuilder()
        .email("newemail@test.com")
        .build();

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(testUser, null, Collections.emptyList()));

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    Assertions.assertNotNull(result);
    verify(userService).updateUser(any(UpdateUserCommand.class));
  }

  @Test
  public void should_update_user_bio_only() {
    UpdateUserInput input = UpdateUserInput.newBuilder()
        .bio("New bio content")
        .build();

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(testUser, null, Collections.emptyList()));

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    Assertions.assertNotNull(result);
    verify(userService).updateUser(any(UpdateUserCommand.class));
  }
}
