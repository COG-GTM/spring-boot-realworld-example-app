package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  public void setUp() {
    userService = new UserService(userRepository, "default-image", passwordEncoder);
  }

  @Test
  public void should_create_user_with_encoded_password_and_default_image() {
    when(passwordEncoder.encode(any())).thenReturn("encoded");
    RegisterParam registerParam = new RegisterParam("email@test.com", "username", "123");

    User user = userService.createUser(registerParam);

    assertEquals("email@test.com", user.getEmail());
    assertEquals("username", user.getUsername());
    assertEquals("encoded", user.getPassword());
    assertEquals("default-image", user.getImage());
    assertEquals("", user.getBio());
    verify(userRepository).save(user);
  }

  @Test
  public void should_update_target_user_and_save_it() {
    User targetUser = new User("email@test.com", "username", "123", "bio", "image");
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("new@test.com")
            .username("newname")
            .password("456")
            .bio("new bio")
            .image("new image")
            .build();

    userService.updateUser(new UpdateUserCommand(targetUser, param));

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertEquals("new@test.com", saved.getEmail());
    assertEquals("newname", saved.getUsername());
    assertEquals("456", saved.getPassword());
    assertEquals("new bio", saved.getBio());
    assertEquals("new image", saved.getImage());
  }
}
