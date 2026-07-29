package io.spring.application.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
  private static final String DEFAULT_IMAGE = "https://static.productionready.io/images/user.jpg";

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Captor private ArgumentCaptor<User> userCaptor;

  private UserService userService;

  @BeforeEach
  public void setUp() {
    userService = new UserService(userRepository, DEFAULT_IMAGE, passwordEncoder);
  }

  @Test
  public void should_create_user_with_encoded_password_and_default_image() {
    when(passwordEncoder.encode("123")).thenReturn("encoded-123");

    User user = userService.createUser(new RegisterParam("aisensiy@test.com", "aisensiy", "123"));

    Assertions.assertEquals("aisensiy@test.com", user.getEmail());
    Assertions.assertEquals("aisensiy", user.getUsername());
    Assertions.assertEquals("encoded-123", user.getPassword());
    Assertions.assertEquals("", user.getBio());
    Assertions.assertEquals(DEFAULT_IMAGE, user.getImage());
    Assertions.assertNotNull(user.getId());

    verify(passwordEncoder).encode("123");
    verify(userRepository).save(userCaptor.capture());
    Assertions.assertEquals(user, userCaptor.getValue());
    Assertions.assertEquals("encoded-123", userCaptor.getValue().getPassword());
  }

  @Test
  public void should_update_user_with_all_command_fields() {
    User user = new User("old@test.com", "old", "123", "old bio", "old image");

    userService.updateUser(
        new UpdateUserCommand(
            user,
            UpdateUserParam.builder()
                .email("new@test.com")
                .username("new")
                .password("new password")
                .bio("new bio")
                .image("new image")
                .build()));

    Assertions.assertEquals("new@test.com", user.getEmail());
    Assertions.assertEquals("new", user.getUsername());
    Assertions.assertEquals("new password", user.getPassword());
    Assertions.assertEquals("new bio", user.getBio());
    Assertions.assertEquals("new image", user.getImage());

    verify(userRepository).save(userCaptor.capture());
    Assertions.assertEquals(user, userCaptor.getValue());
  }

  @Test
  public void should_only_update_provided_fields() {
    User user = new User("old@test.com", "old", "123", "old bio", "old image");

    userService.updateUser(
        new UpdateUserCommand(user, UpdateUserParam.builder().email("new@test.com").build()));

    Assertions.assertEquals("new@test.com", user.getEmail());
    Assertions.assertEquals("old", user.getUsername());
    Assertions.assertEquals("123", user.getPassword());
    Assertions.assertEquals("old bio", user.getBio());
    Assertions.assertEquals("old image", user.getImage());

    verify(userRepository).save(any(User.class));
  }
}
