package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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

  private static final String DEFAULT_IMAGE =
      "https://static.productionready.io/images/smiley-cyrus.jpg";

  private UserService userService;

  @BeforeEach
  public void setUp() {
    userService = new UserService(userRepository, DEFAULT_IMAGE, passwordEncoder);
  }

  @Test
  public void should_create_user_from_register_param() {
    RegisterParam param = new RegisterParam("new@example.com", "newuser", "plain-password");
    when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");

    User user = userService.createUser(param);

    assertThat(user.getEmail()).isEqualTo("new@example.com");
    assertThat(user.getUsername()).isEqualTo("newuser");
    assertThat(user.getBio()).isEqualTo("");
    verify(userRepository).save(user);
  }

  @Test
  public void should_encode_password_and_use_default_image_on_register() {
    RegisterParam param = new RegisterParam("new@example.com", "newuser", "plain-password");
    when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");

    User user = userService.createUser(param);

    assertThat(user.getPassword()).isEqualTo("encoded-password");
    assertThat(user.getImage()).isEqualTo(DEFAULT_IMAGE);
    verify(passwordEncoder).encode("plain-password");
  }

  @Test
  public void should_persist_created_user() {
    RegisterParam param = new RegisterParam("new@example.com", "newuser", "plain-password");
    when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

    userService.createUser(param);

    verify(userRepository).save(captor.capture());
    assertThat(captor.getValue().getEmail()).isEqualTo("new@example.com");
  }

  @Test
  public void should_update_all_provided_fields() {
    User target = new User("old@example.com", "olduser", "pass", "old bio", "old-image");
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("new@example.com")
            .username("newuser")
            .password("newpass")
            .bio("new bio")
            .image("new-image")
            .build();

    userService.updateUser(new UpdateUserCommand(target, param));

    assertThat(target.getEmail()).isEqualTo("new@example.com");
    assertThat(target.getUsername()).isEqualTo("newuser");
    assertThat(target.getPassword()).isEqualTo("newpass");
    assertThat(target.getBio()).isEqualTo("new bio");
    assertThat(target.getImage()).isEqualTo("new-image");
    verify(userRepository).save(target);
  }

  @Test
  public void should_only_update_non_empty_fields() {
    User target = new User("old@example.com", "olduser", "pass", "old bio", "old-image");
    UpdateUserParam param = UpdateUserParam.builder().email("new@example.com").build();

    userService.updateUser(new UpdateUserCommand(target, param));

    assertThat(target.getEmail()).isEqualTo("new@example.com");
    assertThat(target.getUsername()).isEqualTo("olduser");
    assertThat(target.getPassword()).isEqualTo("pass");
    assertThat(target.getBio()).isEqualTo("old bio");
    assertThat(target.getImage()).isEqualTo("old-image");
    verify(userRepository).save(target);
  }

  @Test
  public void should_not_encode_password_on_update() {
    User target = new User("old@example.com", "olduser", "pass", "old bio", "old-image");
    UpdateUserParam param = UpdateUserParam.builder().bio("just bio").build();

    userService.updateUser(new UpdateUserCommand(target, param));

    verify(passwordEncoder, never()).encode(anyString());
  }
}
