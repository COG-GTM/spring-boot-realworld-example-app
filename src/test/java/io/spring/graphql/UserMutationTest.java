package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.Collections;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserMutationTest {

  private UserRepository userRepository;
  private PasswordEncoder encryptService;
  private UserService userService;
  private UserMutation userMutation;

  @BeforeEach
  public void setUp() {
    userRepository = Mockito.mock(UserRepository.class);
    encryptService = Mockito.mock(PasswordEncoder.class);
    userService = Mockito.mock(UserService.class);
    userMutation = new UserMutation(userRepository, encryptService, userService);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_user_success() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("john@jacob.com")
            .username("johnjacob")
            .password("secret")
            .build();
    User user = new User("john@jacob.com", "johnjacob", "encoded", "", "image");
    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertSame(user, result.getLocalContext());
    assertTrue(result.getData() instanceof UserPayload);
    verify(userService).createUser(any(RegisterParam.class));
  }

  @Test
  public void should_return_error_data_when_create_user_violates_constraints() {
    CreateUserInput input =
        CreateUserInput.newBuilder().email("bad").username("johnjacob").password("secret").build();
    when(userService.createUser(any(RegisterParam.class)))
        .thenThrow(new ConstraintViolationException(Collections.emptySet()));

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertTrue(result.getData() instanceof Error);
    assertEquals("BAD_REQUEST", ((Error) result.getData()).getMessage());
    assertNull(result.getLocalContext());
  }

  @Test
  public void should_login_success() {
    String email = "john@jacob.com";
    String password = "secret";
    User user = new User(email, "johnjacob", "encoded", "", "image");
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq(password), eq(user.getPassword()))).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login(password, email);

    assertSame(user, result.getLocalContext());
    assertTrue(result.getData() instanceof UserPayload);
  }

  @Test
  public void should_throw_when_login_user_not_found() {
    String email = "missing@jacob.com";
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.empty());

    assertThrows(InvalidAuthenticationException.class, () -> userMutation.login("secret", email));
  }

  @Test
  public void should_throw_when_login_password_mismatch() {
    String email = "john@jacob.com";
    User user = new User(email, "johnjacob", "encoded", "", "image");
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("wrong"), eq(user.getPassword()))).thenReturn(false);

    assertThrows(InvalidAuthenticationException.class, () -> userMutation.login("wrong", email));
  }

  @Test
  public void should_update_user_success_when_authenticated() {
    User currentUser = new User("john@jacob.com", "johnjacob", "encoded", "", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(currentUser, null, Collections.emptyList()));

    UpdateUserInput input =
        UpdateUserInput.newBuilder()
            .email("new@jacob.com")
            .username("newname")
            .bio("new bio")
            .password("newpass")
            .image("new image")
            .build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    assertSame(currentUser, result.getLocalContext());
    assertTrue(result.getData() instanceof UserPayload);
    verify(userService).updateUser(any(UpdateUserCommand.class));
  }

  @Test
  public void should_return_null_when_update_user_anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    UpdateUserInput input = UpdateUserInput.newBuilder().email("new@jacob.com").build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    assertNull(result);
    verify(userService, never()).updateUser(any(UpdateUserCommand.class));
  }
}
