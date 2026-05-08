package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
  private static final String DEFAULT_IMAGE = "https://default.example/image.png";

  @BeforeEach
  public void setUp() {
    userService = new UserService(userRepository, DEFAULT_IMAGE, passwordEncoder);
  }

  @Test
  public void createUser_should_encode_password_and_save() {
    when(passwordEncoder.encode("plain")).thenReturn("encoded");

    RegisterParam param = new RegisterParam("a@b.com", "alice", "plain");

    User user = userService.createUser(param);

    assertNotNull(user);
    assertEquals("a@b.com", user.getEmail());
    assertEquals("alice", user.getUsername());
    assertEquals("encoded", user.getPassword());
    assertEquals("", user.getBio());
    assertEquals(DEFAULT_IMAGE, user.getImage());
    verify(passwordEncoder, times(1)).encode("plain");
    verify(userRepository, times(1)).save(any(User.class));
  }

  @Test
  public void updateUser_should_apply_changes_and_save() {
    User target = new User("a@b.com", "alice", "secret", "", "");
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("new@b.com")
            .username("alice2")
            .password("new-secret")
            .bio("new bio")
            .image("new-image")
            .build();
    UpdateUserCommand command = new UpdateUserCommand(target, param);

    userService.updateUser(command);

    assertEquals("new@b.com", target.getEmail());
    assertEquals("alice2", target.getUsername());
    assertEquals("new-secret", target.getPassword());
    assertEquals("new bio", target.getBio());
    assertEquals("new-image", target.getImage());
    verify(userRepository, times(1)).save(target);
  }

  @Test
  public void updateUser_should_ignore_blank_fields() {
    User target = new User("a@b.com", "alice", "secret", "old bio", "old-image");
    UpdateUserParam param = UpdateUserParam.builder().build();
    UpdateUserCommand command = new UpdateUserCommand(target, param);

    userService.updateUser(command);

    assertEquals("a@b.com", target.getEmail());
    assertEquals("alice", target.getUsername());
    assertEquals("secret", target.getPassword());
    assertEquals("old bio", target.getBio());
    assertEquals("old-image", target.getImage());
    verify(userRepository, times(1)).save(target);
  }
}
