package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserServiceTest {

  private static final String DEFAULT_IMAGE = "https://example.com/default.png";

  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private UserService userService;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    userService = new UserService(userRepository, DEFAULT_IMAGE, passwordEncoder);
  }

  @Test
  public void should_create_user_with_encoded_password_and_default_image() {
    when(passwordEncoder.encode("plain")).thenReturn("encoded");

    User user = userService.createUser(new RegisterParam("john@jacob.com", "johnjacob", "plain"));

    assertNotNull(user.getId());
    assertEquals("john@jacob.com", user.getEmail());
    assertEquals("johnjacob", user.getUsername());
    assertEquals("encoded", user.getPassword());
    assertEquals("", user.getBio());
    assertEquals(DEFAULT_IMAGE, user.getImage());
    verify(userRepository).save(user);
  }

  @Test
  public void should_update_user_and_persist() {
    User user = new User("john@jacob.com", "johnjacob", "123", "bio", "img");
    UpdateUserParam param = UpdateUserParam.builder().email("new@jacob.com").bio("new bio").build();

    userService.updateUser(new UpdateUserCommand(user, param));

    assertEquals("new@jacob.com", user.getEmail());
    assertEquals("johnjacob", user.getUsername());
    assertEquals("123", user.getPassword());
    assertEquals("new bio", user.getBio());
    assertEquals("img", user.getImage());
    verify(userRepository).save(user);
  }
}
