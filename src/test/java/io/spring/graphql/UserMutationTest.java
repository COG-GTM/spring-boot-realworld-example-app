package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

  @InjectMocks private UserMutation userMutation;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("email@example.com", "username", "encoded", "", "");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_user() {
    when(userService.createUser(any())).thenReturn(user);

    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("email@example.com")
            .username("username")
            .password("123")
            .build();
    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    Assertions.assertEquals(user, result.getLocalContext());
    Assertions.assertTrue(result.getData() instanceof UserPayload);
  }

  @Test
  public void should_return_errors_as_data_on_constraint_violation() {
    when(userService.createUser(any()))
        .thenThrow(new ConstraintViolationException(Collections.emptySet()));

    CreateUserInput input =
        CreateUserInput.newBuilder().email("bad").username("u").password("p").build();
    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    Assertions.assertTrue(result.getData() instanceof Error);
    Assertions.assertNull(result.getLocalContext());
  }

  @Test
  public void should_login_with_valid_credentials() {
    when(userRepository.findByEmail("email@example.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("123", "encoded")).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("123", "email@example.com");

    Assertions.assertEquals(user, result.getLocalContext());
  }

  @Test
  public void should_reject_login_with_wrong_password() {
    when(userRepository.findByEmail("email@example.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("wrong", "encoded")).thenReturn(false);

    Assertions.assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("wrong", "email@example.com"));
  }

  @Test
  public void should_reject_login_with_unknown_email() {
    when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

    Assertions.assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("123", "missing@example.com"));
  }

  @Test
  public void should_update_current_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    UpdateUserInput input =
        UpdateUserInput.newBuilder().username("newname").email("new@example.com").build();
    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    verify(userService).updateUser(any());
    Assertions.assertEquals(user, result.getLocalContext());
  }

  @Test
  public void should_return_null_when_updating_anonymously() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    Assertions.assertNull(userMutation.updateUser(UpdateUserInput.newBuilder().build()));
  }
}
