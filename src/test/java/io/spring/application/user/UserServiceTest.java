package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserServiceTest {

  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private UserService userService;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    userService = new UserService(userRepository, "default-image", passwordEncoder);
  }

  @Test
  void should_create_user_with_encoded_password_and_default_image() {
    when(passwordEncoder.encode("plain")).thenReturn("encoded");
    RegisterParam param = new RegisterParam("new@example.com", "newuser", "plain");

    User user = userService.createUser(param);

    assertEquals("new@example.com", user.getEmail());
    assertEquals("newuser", user.getUsername());
    assertEquals("encoded", user.getPassword());
    assertEquals("default-image", user.getImage());
    verify(userRepository).save(user);
  }

  @Test
  void should_update_user_fields() {
    User user = new User("old@example.com", "old", "encoded", "old bio", "old-image");
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("new@example.com")
            .username("newname")
            .bio("new bio")
            .image("new-image")
            .build();

    userService.updateUser(new UpdateUserCommand(user, param));

    assertEquals("new@example.com", user.getEmail());
    assertEquals("newname", user.getUsername());
    assertEquals("new bio", user.getBio());
    assertEquals("new-image", user.getImage());
    verify(userRepository).save(user);
  }
}
