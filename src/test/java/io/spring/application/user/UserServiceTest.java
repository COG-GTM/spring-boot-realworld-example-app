package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, "https://default-image.com/avatar.png", passwordEncoder);
  }

  @Test
  void should_create_user_successfully() {
    when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");

    RegisterParam param = new RegisterParam("user@example.com", "username", "plainPassword");

    User user = userService.createUser(param);

    assertNotNull(user);
    assertEquals("user@example.com", user.getEmail());
    assertEquals("username", user.getUsername());
    assertEquals("encoded_password", user.getPassword());
    assertEquals("https://default-image.com/avatar.png", user.getImage());
    assertEquals("", user.getBio());
    verify(passwordEncoder).encode("plainPassword");
    verify(userRepository).save(any(User.class));
  }

  @Test
  void should_encode_password_when_creating_user() {
    when(passwordEncoder.encode("mySecret")).thenReturn("hashed_mySecret");

    RegisterParam param = new RegisterParam("a@b.com", "user1", "mySecret");
    User user = userService.createUser(param);

    assertEquals("hashed_mySecret", user.getPassword());
    verify(passwordEncoder).encode("mySecret");
  }

  @Test
  void should_update_user_successfully() {
    User existingUser = new User("old@example.com", "olduser", "oldpass", "old bio", "old.png");
    UpdateUserParam updateParam =
        UpdateUserParam.builder()
            .email("new@example.com")
            .username("newuser")
            .password("newpass")
            .bio("new bio")
            .image("new.png")
            .build();
    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);

    userService.updateUser(command);

    assertEquals("new@example.com", existingUser.getEmail());
    assertEquals("newuser", existingUser.getUsername());
    assertEquals("newpass", existingUser.getPassword());
    assertEquals("new bio", existingUser.getBio());
    assertEquals("new.png", existingUser.getImage());
    verify(userRepository).save(existingUser);
  }

  @Test
  void should_not_change_fields_when_update_with_empty_values() {
    User existingUser = new User("old@example.com", "olduser", "oldpass", "bio", "image.png");
    UpdateUserParam updateParam =
        UpdateUserParam.builder().build();
    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);

    userService.updateUser(command);

    assertEquals("old@example.com", existingUser.getEmail());
    assertEquals("olduser", existingUser.getUsername());
    assertEquals("oldpass", existingUser.getPassword());
    assertEquals("bio", existingUser.getBio());
    assertEquals("image.png", existingUser.getImage());
    verify(userRepository).save(existingUser);
  }

  @Test
  void should_update_only_email() {
    User existingUser = new User("old@example.com", "user1", "pass", "bio", "img.png");
    UpdateUserParam updateParam = UpdateUserParam.builder().email("new@example.com").build();
    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);

    userService.updateUser(command);

    assertEquals("new@example.com", existingUser.getEmail());
    assertEquals("user1", existingUser.getUsername());
    assertEquals("pass", existingUser.getPassword());
    verify(userRepository).save(existingUser);
  }
}
