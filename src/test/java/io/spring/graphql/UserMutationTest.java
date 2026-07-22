package io.spring.graphql;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import io.spring.graphql.types.Error;
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.Collections;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserMutationTest extends GraphQLTestBase {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder encryptService;
  @Mock private UserService userService;

  private UserMutation userMutation;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    userMutation = new UserMutation(userRepository, encryptService, userService);
  }

  @Test
  public void should_create_user_success() {
    User user = new User("email@test.com", "username", "pass", "", "");
    CreateUserInput input = new CreateUserInput("email@test.com", "username", "pass");
    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertThat(result.getData(), instanceOf(UserPayload.class));
    assertThat(result.getLocalContext(), is(user));
  }

  @Test
  public void should_return_error_result_when_create_user_fails_validation() {
    CreateUserInput input = new CreateUserInput("bad", "username", "pass");
    when(userService.createUser(any(RegisterParam.class)))
        .thenThrow(new ConstraintViolationException(Collections.emptySet()));

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertThat(result.getData(), instanceOf(Error.class));
  }

  @Test
  public void should_login_success_with_matching_password() {
    User user = new User("email@test.com", "username", "encoded", "", "");
    when(userRepository.findByEmail(eq("email@test.com"))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("pass"), eq("encoded"))).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("pass", "email@test.com");

    assertThat(result.getData(), instanceOf(UserPayload.class));
    assertThat(result.getLocalContext(), is(user));
  }

  @Test
  public void should_throw_when_login_password_does_not_match() {
    User user = new User("email@test.com", "username", "encoded", "", "");
    when(userRepository.findByEmail(eq("email@test.com"))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("wrong"), eq("encoded"))).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class, () -> userMutation.login("wrong", "email@test.com"));
  }

  @Test
  public void should_throw_when_login_user_not_found() {
    when(userRepository.findByEmail(eq("missing@test.com"))).thenReturn(Optional.empty());
    assertThrows(
        InvalidAuthenticationException.class, () -> userMutation.login("pass", "missing@test.com"));
  }

  @Test
  public void should_update_current_user() {
    User user = new User("email@test.com", "username", "pass", "", "");
    setCurrentUser(user);
    UpdateUserInput input =
        UpdateUserInput.newBuilder()
            .email("new@test.com")
            .username("newname")
            .bio("newbio")
            .password("newpass")
            .image("newimage")
            .build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    assertThat(result.getData(), instanceOf(UserPayload.class));
    assertThat(result.getLocalContext(), is(user));
    verify(userService).updateUser(any(UpdateUserCommand.class));
  }

  @Test
  public void should_return_null_when_update_user_anonymous() {
    setAnonymous();
    UpdateUserInput input = UpdateUserInput.newBuilder().email("new@test.com").build();
    assertThat(userMutation.updateUser(input) == null, is(true));
  }
}
