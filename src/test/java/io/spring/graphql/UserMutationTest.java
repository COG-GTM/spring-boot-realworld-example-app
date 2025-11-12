package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserMutationTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private UserService userService;

  @InjectMocks private UserMutation userMutation;

  private String email;
  private String username;
  private String password;
  private User user;

  @BeforeEach
  public void setUp() {
    email = "test@example.com";
    username = "testuser";
    password = "password123";
    user = new User(email, username, password, "bio", "image");
  }

  @Test
  public void should_create_user_successfully() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email(email)
            .username(username)
            .password(password)
            .build();

    when(userService.createUser(any())).thenReturn(user);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertThat(result).isNotNull();
    assertThat(result.getData()).isInstanceOf(UserPayload.class);
    assertThat(result.getLocalContext()).isEqualTo(user);
    verify(userService).createUser(any());
  }

  @Test
  public void should_login_successfully() {
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq(password), any())).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login(password, email);

    assertThat(result).isNotNull();
    assertThat(result.getData()).isNotNull();
    assertThat(result.getLocalContext()).isEqualTo(user);
  }

  @Test
  public void should_fail_login_with_wrong_password() {
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("wrongpassword"), any())).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class, () -> userMutation.login("wrongpassword", email));
  }

  @Test
  public void should_fail_login_with_nonexistent_email() {
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.empty());

    assertThrows(InvalidAuthenticationException.class, () -> userMutation.login(password, email));
  }
}
