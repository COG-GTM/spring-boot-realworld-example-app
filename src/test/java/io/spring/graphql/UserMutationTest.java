package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.InvalidAuthenticationException;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserMutationTest extends GraphQLTestBase {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder encryptService;
  @Mock private UserService userService;

  private UserMutation userMutation;
  private User user;

  @BeforeEach
  void setUp() {
    userMutation = new UserMutation(userRepository, encryptService, userService);
    user = newUser();
  }

  @Test
  void should_create_user() {
    when(userService.createUser(any())).thenReturn(user);

    DataFetcherResult<UserResult> result =
        userMutation.createUser(
            CreateUserInput.newBuilder()
                .email("john@jacob.com")
                .username("johnjacob")
                .password("123")
                .build());

    assertThat(result.getData()).isInstanceOf(UserPayload.class);
    assertThat(result.getLocalContext()).isSameAs(user);
  }

  @Test
  void should_return_error_data_when_create_user_invalid() {
    when(userService.createUser(any()))
        .thenThrow(new ConstraintViolationException("invalid", Collections.emptySet()));

    DataFetcherResult<UserResult> result =
        userMutation.createUser(
            CreateUserInput.newBuilder().email("bad").username("u").password("p").build());

    assertThat(result.getData()).isInstanceOf(Error.class);
    assertThat(((Error) result.getData()).getMessage()).isEqualTo("BAD_REQUEST");
  }

  @Test
  void should_login_with_valid_credentials() {
    when(userRepository.findByEmail(eq("john@jacob.com"))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("123"), eq(user.getPassword()))).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("123", "john@jacob.com");

    assertThat(result.getLocalContext()).isSameAs(user);
  }

  @Test
  void should_throw_when_login_email_unknown() {
    when(userRepository.findByEmail(eq("nobody@test.com"))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userMutation.login("123", "nobody@test.com"))
        .isInstanceOf(InvalidAuthenticationException.class);
  }

  @Test
  void should_throw_when_login_password_wrong() {
    when(userRepository.findByEmail(eq("john@jacob.com"))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("wrong"), eq(user.getPassword()))).thenReturn(false);

    assertThatThrownBy(() -> userMutation.login("wrong", "john@jacob.com"))
        .isInstanceOf(InvalidAuthenticationException.class);
  }

  @Test
  void should_update_user() {
    setCurrentUser(user);

    DataFetcherResult<UserPayload> result =
        userMutation.updateUser(
            UpdateUserInput.newBuilder()
                .email("new@test.com")
                .username("newname")
                .bio("bio")
                .password("pw")
                .image("img")
                .build());

    assertThat(result.getLocalContext()).isSameAs(user);
    verify(userService).updateUser(any(UpdateUserCommand.class));
  }

  @Test
  void should_return_null_when_update_user_anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertThat(userMutation.updateUser(UpdateUserInput.newBuilder().build())).isNull();
  }

  @Test
  void should_return_null_when_update_user_principal_null() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(null, null));

    assertThat(userMutation.updateUser(UpdateUserInput.newBuilder().build())).isNull();
  }
}
