package io.spring.application.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    when(passwordEncoder.encode("123")).thenReturn("encoded");

    User user = userService.createUser(new RegisterParam("a@test.com", "a", "123"));

    Assertions.assertEquals("a@test.com", user.getEmail());
    Assertions.assertEquals("a", user.getUsername());
    Assertions.assertEquals("encoded", user.getPassword());
    Assertions.assertEquals("", user.getBio());
    Assertions.assertEquals(DEFAULT_IMAGE, user.getImage());
    verify(userRepository).save(user);
  }

  @Test
  public void should_update_user_and_save() {
    User user = new User("a@test.com", "a", "123", "", "");
    UpdateUserParam param =
        UpdateUserParam.builder().email("b@test.com").username("b").bio("bio").build();

    userService.updateUser(new UpdateUserCommand(user, param));

    Assertions.assertEquals("b@test.com", user.getEmail());
    Assertions.assertEquals("b", user.getUsername());
    Assertions.assertEquals("123", user.getPassword());
    Assertions.assertEquals("bio", user.getBio());
    verify(userRepository).save(user);
    verify(passwordEncoder, never()).encode(any());
  }
}
