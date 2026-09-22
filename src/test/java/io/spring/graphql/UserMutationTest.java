package io.spring.graphql;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.CreateUserInput;
import io.spring.graphql.types.UpdateUserInput;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder encryptService;
  @Mock private UserService userService;

  private UserMutation userMutation;
  private final User user = new User("email@test.com", "username", "encoded", "", "");

  @BeforeEach
  public void setUp() {
    userMutation = new UserMutation(userRepository, encryptService, userService);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_user() {
    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("email@test.com")
            .username("username")
            .password("123")
            .build();

    assertEquals(user, userMutation.createUser(input).getLocalContext());

    ArgumentCaptor<RegisterParam> captor = ArgumentCaptor.forClass(RegisterParam.class);
    verify(userService).createUser(captor.capture());
    assertEquals("email@test.com", captor.getValue().getEmail());
    assertEquals("username", captor.getValue().getUsername());
    assertEquals("123", captor.getValue().getPassword());
  }

  @Test
  public void should_login_with_matching_password() {
    when(userRepository.findByEmail("email@test.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("123", "encoded")).thenReturn(true);

    assertEquals(user, userMutation.login("123", "email@test.com").getLocalContext());
  }

  @Test
  public void should_not_login_with_wrong_password() {
    when(userRepository.findByEmail("email@test.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("wrong", "encoded")).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class, () -> userMutation.login("wrong", "email@test.com"));
  }

  @Test
  public void should_not_login_unknown_email() {
    when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

    assertThrows(
        InvalidAuthenticationException.class, () -> userMutation.login("123", "unknown@test.com"));
  }

  @Test
  public void should_update_current_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, emptyList()));
    UpdateUserInput changes =
        UpdateUserInput.newBuilder()
            .email("new@test.com")
            .username("newname")
            .bio("new bio")
            .password("456")
            .image("new image")
            .build();

    assertEquals(user, userMutation.updateUser(changes).getLocalContext());

    ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
    verify(userService).updateUser(captor.capture());
    assertEquals(user, captor.getValue().getTargetUser());
    assertEquals("new@test.com", captor.getValue().getParam().getEmail());
    assertEquals("newname", captor.getValue().getParam().getUsername());
  }

  @Test
  public void should_not_update_user_for_anonymous_request() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(null, null, emptyList()));

    assertNull(userMutation.updateUser(UpdateUserInput.newBuilder().build()));
  }
}
