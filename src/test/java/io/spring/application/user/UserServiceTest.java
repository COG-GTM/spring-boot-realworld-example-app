package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    userService =
        new UserService(userRepository, "https://default-image.com/avatar.png", passwordEncoder);
  }

  @Test
  void createUser_should_encode_password_and_save() {
    RegisterParam param = new RegisterParam("e@t.com", "username", "rawpass");
    when(passwordEncoder.encode("rawpass")).thenReturn("encodedpass");

    User result = userService.createUser(param);

    assertNotNull(result);
    assertEquals("e@t.com", result.getEmail());
    assertEquals("username", result.getUsername());
    assertEquals("encodedpass", result.getPassword());
    assertEquals("", result.getBio());
    assertEquals("https://default-image.com/avatar.png", result.getImage());
    verify(passwordEncoder).encode("rawpass");
    verify(userRepository).save(any(User.class));
  }

  @Test
  void updateUser_should_update_fields_and_save() {
    User user = new User("old@t.com", "olduser", "oldpass", "oldbio", "oldimg");
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("new@t.com")
            .username("newuser")
            .password("")
            .bio("newbio")
            .image("newimg")
            .build();
    UpdateUserCommand command = new UpdateUserCommand(user, param);

    userService.updateUser(command);

    assertEquals("new@t.com", user.getEmail());
    assertEquals("newuser", user.getUsername());
    assertEquals("newbio", user.getBio());
    assertEquals("newimg", user.getImage());
    verify(userRepository).save(user);
  }
}
