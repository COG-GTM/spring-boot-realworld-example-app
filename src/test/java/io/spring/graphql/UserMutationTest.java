package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserMutationTest {

  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private UserService userService;
  private UserMutation userMutation;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    userService = mock(UserService.class);
    userMutation = new UserMutation(userRepository, passwordEncoder, userService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_user_successfully() {
    CreateUserInput input = CreateUserInput.newBuilder()
        .email("test@example.com")
        .username("testuser")
        .password("password123")
        .build();
    User user = new User("test@example.com", "testuser", "encoded", "", "");
    when(userService.createUser(any())).thenReturn(user);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);
    assertNotNull(result);
    assertNotNull(result.getData());
  }

  @Test
  public void should_login_successfully() {
    User user = new User("test@example.com", "testuser", "encoded", "", "");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password", "encoded")).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("password", "test@example.com");
    assertNotNull(result);
    assertNotNull(result.getData());
  }

  @Test
  public void should_throw_on_invalid_login() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
    assertThrows(InvalidAuthenticationException.class, () -> {
      userMutation.login("password", "test@example.com");
    });
  }

  @Test
  public void should_throw_on_wrong_password() {
    User user = new User("test@example.com", "testuser", "encoded", "", "");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);
    assertThrows(InvalidAuthenticationException.class, () -> {
      userMutation.login("wrong", "test@example.com");
    });
  }

  @Test
  public void should_update_user_when_authenticated() {
    User user = new User("test@example.com", "testuser", "pass", "", "");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);

    UpdateUserInput input = UpdateUserInput.newBuilder()
        .username("newname")
        .email("new@email.com")
        .bio("new bio")
        .password("newpass")
        .image("new.jpg")
        .build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);
    assertNotNull(result);
    verify(userService).updateUser(any());
  }

  @Test
  public void should_return_null_when_anonymous() {
    AnonymousAuthenticationToken auth =
        new AnonymousAuthenticationToken(
            "key", "anonymous",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    UpdateUserInput input = UpdateUserInput.newBuilder()
        .username("newname")
        .email("new@email.com")
        .bio("")
        .password("")
        .image("")
        .build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);
    assertNull(result);
  }
}
