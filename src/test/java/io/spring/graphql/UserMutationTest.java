package io.spring.graphql;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.Collections;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
  @Mock private PasswordEncoder encryptService;
  @Mock private UserService userService;

  private UserMutation userMutation;
  private User user;

  @BeforeEach
  public void setUp() {
    userMutation = new UserMutation(userRepository, encryptService, userService);
    user = new User("user@test.com", "testuser", "encoded-password", "", "");
  }

  @AfterEach
  public void cleanup() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthenticated(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
  }

  @Test
  public void should_create_user_successfully() {
    when(userService.createUser(any())).thenReturn(user);

    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("user@test.com")
            .username("testuser")
            .password("password")
            .build();

    DataFetcherResult<UserResult> result = userMutation.createUser(input);
    assertThat(result, notNullValue());
    assertThat(result.getData(), notNullValue());
  }

  @Test
  public void should_return_errors_on_invalid_registration() {
    when(userService.createUser(any()))
        .thenThrow(new ConstraintViolationException("validation failed", Collections.emptySet()));

    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("invalid")
            .username("")
            .password("pass")
            .build();

    DataFetcherResult<UserResult> result = userMutation.createUser(input);
    assertThat(result, notNullValue());
  }

  @Test
  public void should_login_with_correct_credentials() {
    when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("password", "encoded-password")).thenReturn(true);

    DataFetcherResult<UserPayload> result =
        userMutation.login("password", "user@test.com");
    assertThat(result, notNullValue());
  }

  @Test
  public void should_fail_login_with_wrong_credentials() {
    when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("wrong", "encoded-password")).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("wrong", "user@test.com"));
  }

  @Test
  public void should_update_user_when_authenticated() {
    setAuthenticated(user);

    UpdateUserInput input =
        UpdateUserInput.newBuilder().email("new@test.com").username("newname").build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);
    assertThat(result, notNullValue());
  }

  @Test
  public void should_return_null_when_not_authenticated_on_update() {
    AnonymousAuthenticationToken anonymous =
        new AnonymousAuthenticationToken(
            "key",
            "anonymous",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymous);

    UpdateUserInput input = UpdateUserInput.newBuilder().email("new@test.com").build();
    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);
    assertThat(result, is(nullValue()));
  }
}
