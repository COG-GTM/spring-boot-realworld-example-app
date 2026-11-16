package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
  private static final String DEFAULT_IMAGE = "https://example.com/default.png";

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private UserService userService;

  @BeforeEach
  public void setUp() {
    userService = new UserService(userRepository, DEFAULT_IMAGE, passwordEncoder);
  }

  @Test
  public void should_create_user_with_encoded_password_and_default_image() {
    when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");

    User user = userService.createUser(new RegisterParam("john@jacob.com", "johnjacob", "secret"));

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertSame(user, captor.getValue());
    assertNotNull(user.getId());
    assertEquals("john@jacob.com", user.getEmail());
    assertEquals("johnjacob", user.getUsername());
    assertEquals("encoded-secret", user.getPassword());
    assertEquals("", user.getBio());
    assertEquals(DEFAULT_IMAGE, user.getImage());
  }

  @Test
  public void should_update_all_user_fields_and_save() {
    User user = new User("old@example.com", "old", "old-password", "old bio", "old.png");
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("new@example.com")
            .username("new")
            .password("new-password")
            .bio("new bio")
            .image("new.png")
            .build();

    userService.updateUser(new UpdateUserCommand(user, param));

    assertEquals("new@example.com", user.getEmail());
    assertEquals("new", user.getUsername());
    assertEquals("new-password", user.getPassword());
    assertEquals("new bio", user.getBio());
    assertEquals("new.png", user.getImage());
    verify(userRepository).save(user);
  }

  @Test
  public void should_keep_existing_values_for_empty_update_fields() {
    User user = new User("old@example.com", "old", "old-password", "old bio", "old.png");
    UpdateUserParam param = UpdateUserParam.builder().bio("only bio changed").build();

    userService.updateUser(new UpdateUserCommand(user, param));

    assertEquals("old@example.com", user.getEmail());
    assertEquals("old", user.getUsername());
    assertEquals("old-password", user.getPassword());
    assertEquals("only bio changed", user.getBio());
    assertEquals("old.png", user.getImage());
    verify(userRepository).save(user);
  }
}
