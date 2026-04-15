package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService =
        new UserService(
            userRepository,
            "https://static.productionready.io/images/smiley-cyrus.jpg",
            passwordEncoder);
  }

  @Test
  void should_create_user_with_encoded_password() {
    RegisterParam registerParam = new RegisterParam("test@example.com", "testuser", "password123");
    when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

    User user = userService.createUser(registerParam);

    assertNotNull(user);
    assertEquals("test@example.com", user.getEmail());
    assertEquals("testuser", user.getUsername());
    verify(userRepository).save(any(User.class));
  }

  @Test
  void should_create_user_with_default_image() {
    RegisterParam registerParam = new RegisterParam("test@example.com", "testuser", "pass");
    when(passwordEncoder.encode("pass")).thenReturn("encodedPass");

    User user = userService.createUser(registerParam);

    assertEquals("https://static.productionready.io/images/smiley-cyrus.jpg", user.getImage());
  }

  @Test
  void should_save_user_to_repository() {
    RegisterParam registerParam = new RegisterParam("test@example.com", "testuser", "pass");
    when(passwordEncoder.encode("pass")).thenReturn("encodedPass");

    userService.createUser(registerParam);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    User savedUser = userCaptor.getValue();
    assertEquals("test@example.com", savedUser.getEmail());
    assertEquals("testuser", savedUser.getUsername());
  }
}
