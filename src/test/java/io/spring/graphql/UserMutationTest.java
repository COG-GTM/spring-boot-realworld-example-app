package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserMutationTest {

  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private UserService userService;
  private UserMutation userMutation;
  private User user;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    userService = mock(UserService.class);
    userMutation = new UserMutation(userRepository, passwordEncoder, userService);
    user = new User("test@test.com", "testuser", "password", "", "");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_create_user() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("new@test.com")
            .username("newuser")
            .password("password")
            .build();
    User newUser = new User("new@test.com", "newuser", "encoded", "", "");
    when(userService.createUser(any())).thenReturn(newUser);

    var result = userMutation.createUser(input);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertTrue(result.getData() instanceof UserPayload);
  }

  @Test
  void should_login_successfully() {
    when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password", user.getPassword())).thenReturn(true);

    var result = userMutation.login("password", "test@test.com");

    assertNotNull(result);
  }

  @Test
  void should_throw_on_invalid_login() {
    when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("wrong", "test@test.com"));
  }

  @Test
  void should_throw_on_login_user_not_found() {
    when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("pass", "missing@test.com"));
  }

  @Test
  void should_update_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    UpdateUserInput input =
        UpdateUserInput.newBuilder().username("updated").email("updated@test.com").build();

    var result = userMutation.updateUser(input);

    assertNotNull(result);
    verify(userService).updateUser(any());
  }

  @Test
  void should_return_null_for_anonymous_update() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new org.springframework.security.authentication.AnonymousAuthenticationToken(
                "key",
                "anon",
                Collections.singletonList(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_ANONYMOUS"))));

    UpdateUserInput input = UpdateUserInput.newBuilder().username("updated").build();
    var result = userMutation.updateUser(input);

    assertNull(result);
  }
}
