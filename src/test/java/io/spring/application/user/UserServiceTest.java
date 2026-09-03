package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

  @BeforeEach
  public void setUp() {
    userService = new UserService(userRepository, "default.png", passwordEncoder);
  }

  @Test
  public void should_create_user() {
    when(passwordEncoder.encode("123")).thenReturn("encoded");
    RegisterParam param = new RegisterParam("email", "username", "123");

    User user = userService.createUser(param);

    assertEquals("email", user.getEmail());
    assertEquals("username", user.getUsername());
    assertEquals("encoded", user.getPassword());
    assertEquals("", user.getBio());
    assertEquals("default.png", user.getImage());
    verify(userRepository).save(user);
  }

  @Test
  public void should_update_user_and_keep_untouched_fields() {
    User user = new User("email", "username", "password", "bio", "image");
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("new email")
            .username("new username")
            .bio("new bio")
            .build();

    userService.updateUser(new UpdateUserCommand(user, param));

    assertEquals("new email", user.getEmail());
    assertEquals("new username", user.getUsername());
    assertEquals("password", user.getPassword());
    assertEquals("new bio", user.getBio());
    assertEquals("image", user.getImage());
    verify(userRepository).save(user);
  }
}
