package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  private static final String DEFAULT_IMAGE = "https://example.com/default.jpg";

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  private UserService userService;

  @BeforeEach
  public void setUp() {
    userService = new UserService(userRepository, DEFAULT_IMAGE, passwordEncoder);
  }

  @Test
  public void should_create_user_with_encoded_password_and_default_image() {
    RegisterParam param = new RegisterParam("new@example.com", "newuser", "plain-password");
    when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");

    User user = userService.createUser(param);

    assertEquals("new@example.com", user.getEmail());
    assertEquals("newuser", user.getUsername());
    assertEquals("encoded-password", user.getPassword());
    assertEquals("", user.getBio());
    assertEquals(DEFAULT_IMAGE, user.getImage());

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(passwordEncoder, times(1)).encode(eq("plain-password"));
    verify(userRepository, times(1)).save(captor.capture());
    assertSame(user, captor.getValue());
  }

  @Test
  public void should_update_target_user_fields_and_save_it() {
    User target = new User("old@example.com", "olduser", "password", "old bio", "old image");
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("new@example.com")
            .username("newuser")
            .password("new-password")
            .bio("new bio")
            .image("new image")
            .build();
    UpdateUserCommand command = new UpdateUserCommand(target, param);

    userService.updateUser(command);

    assertEquals("new@example.com", target.getEmail());
    assertEquals("newuser", target.getUsername());
    assertEquals("new-password", target.getPassword());
    assertEquals("new bio", target.getBio());
    assertEquals("new image", target.getImage());
    verify(userRepository, times(1)).save(target);
  }

  @Test
  public void should_keep_original_fields_when_update_param_is_empty() {
    User target = new User("old@example.com", "olduser", "password", "old bio", "old image");
    UpdateUserParam param = UpdateUserParam.builder().build();
    UpdateUserCommand command = new UpdateUserCommand(target, param);

    userService.updateUser(command);

    assertEquals("old@example.com", target.getEmail());
    assertEquals("olduser", target.getUsername());
    assertEquals("password", target.getPassword());
    assertEquals("old bio", target.getBio());
    assertEquals("old image", target.getImage());
    verify(userRepository, times(1)).save(target);
  }
}
