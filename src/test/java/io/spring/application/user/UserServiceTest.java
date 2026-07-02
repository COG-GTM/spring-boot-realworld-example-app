package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
  public void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    userService = new UserService(userRepository, "default.png", passwordEncoder);
  }

  @Test
  public void should_create_user_with_encoded_password_and_default_image() {
    when(passwordEncoder.encode("secret")).thenReturn("encoded");
    RegisterParam param = new RegisterParam("jane@example.com", "jane", "secret");

    User user = userService.createUser(param);

    assertEquals("jane@example.com", user.getEmail());
    assertEquals("jane", user.getUsername());
    assertEquals("encoded", user.getPassword());
    assertEquals("default.png", user.getImage());
    verify(userRepository).save(any(User.class));
  }

  @Test
  public void should_update_target_user_and_save() {
    User target = new User("old@example.com", "old", "pw", "bio", "img");
    UpdateUserParam param =
        UpdateUserParam.builder().email("new@example.com").username("new").build();

    userService.updateUser(new UpdateUserCommand(target, param));

    assertEquals("new@example.com", target.getEmail());
    assertEquals("new", target.getUsername());
    verify(userRepository).save(target);
  }
}
