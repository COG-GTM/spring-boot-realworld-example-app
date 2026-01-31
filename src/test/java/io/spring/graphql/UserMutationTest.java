package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.user.RegisterParam;
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

  @Mock private PasswordEncoder encryptService;

  @Mock private UserService userService;

  @InjectMocks private UserMutation userMutation;

  private User testUser;

  @BeforeEach
  public void setUp() {
    testUser = new User("test@example.com", "testuser", "password", "bio", "image");
  }

  @Test
  public void should_create_user_successfully() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("test@example.com")
            .username("testuser")
            .password("password")
            .build();

    when(userService.createUser(any(RegisterParam.class))).thenReturn(testUser);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(testUser, result.getLocalContext());
    verify(userService).createUser(any(RegisterParam.class));
  }

  @Test
  public void should_login_successfully() {
    when(userRepository.findByEmail(eq("test@example.com"))).thenReturn(Optional.of(testUser));
    when(encryptService.matches(eq("password"), eq(testUser.getPassword()))).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("password", "test@example.com");

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(testUser, result.getLocalContext());
  }

  @Test
  public void should_fail_login_with_wrong_password() {
    when(userRepository.findByEmail(eq("test@example.com"))).thenReturn(Optional.of(testUser));
    when(encryptService.matches(eq("wrongpassword"), eq(testUser.getPassword())))
        .thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("wrongpassword", "test@example.com"));
  }

  @Test
  public void should_fail_login_with_non_existent_email() {
    when(userRepository.findByEmail(eq("nonexistent@example.com"))).thenReturn(Optional.empty());

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("password", "nonexistent@example.com"));
  }
}
