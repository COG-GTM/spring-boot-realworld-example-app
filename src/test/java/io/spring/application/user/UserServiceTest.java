package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  private static final String DEFAULT_IMAGE = "http://default.image/avatar.png";

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Captor private ArgumentCaptor<User> userCaptor;

  private UserService userService() {
    return new UserService(userRepository, DEFAULT_IMAGE, passwordEncoder);
  }

  @Test
  public void should_create_user_with_encoded_password_and_default_image() {
    when(passwordEncoder.encode("123")).thenReturn("encoded-123");

    User user = userService().createUser(new RegisterParam("john@example.com", "john", "123"));

    verify(userRepository).save(userCaptor.capture());
    assertThat(userCaptor.getValue()).isSameAs(user);
    assertThat(user.getEmail()).isEqualTo("john@example.com");
    assertThat(user.getUsername()).isEqualTo("john");
    assertThat(user.getPassword()).isEqualTo("encoded-123");
    assertThat(user.getBio()).isEmpty();
    assertThat(user.getImage()).isEqualTo(DEFAULT_IMAGE);
    assertThat(user.getId()).isNotBlank();
  }

  @Test
  public void should_update_all_user_fields() {
    User target = new User("old@example.com", "old", "old-password", "old bio", "old image");

    userService()
        .updateUser(
            new UpdateUserCommand(
                target,
                UpdateUserParam.builder()
                    .email("new@example.com")
                    .username("new")
                    .password("new-password")
                    .bio("new bio")
                    .image("new image")
                    .build()));

    verify(userRepository).save(userCaptor.capture());
    User saved = userCaptor.getValue();
    assertThat(saved).isSameAs(target);
    assertThat(saved.getEmail()).isEqualTo("new@example.com");
    assertThat(saved.getUsername()).isEqualTo("new");
    assertThat(saved.getPassword()).isEqualTo("new-password");
    assertThat(saved.getBio()).isEqualTo("new bio");
    assertThat(saved.getImage()).isEqualTo("new image");
  }

  @Test
  public void should_only_update_provided_user_fields() {
    User target = new User("old@example.com", "old", "old-password", "old bio", "old image");

    userService()
        .updateUser(
            new UpdateUserCommand(
                target, UpdateUserParam.builder().email("new@example.com").build()));

    verify(userRepository).save(target);
    assertThat(target.getEmail()).isEqualTo("new@example.com");
    assertThat(target.getUsername()).isEqualTo("old");
    assertThat(target.getPassword()).isEqualTo("old-password");
    assertThat(target.getBio()).isEqualTo("old bio");
    assertThat(target.getImage()).isEqualTo("old image");
  }

  @Test
  public void should_keep_user_unchanged_when_no_field_is_provided() {
    User target = new User("old@example.com", "old", "old-password", "old bio", "old image");

    userService().updateUser(new UpdateUserCommand(target, UpdateUserParam.builder().build()));

    verify(userRepository).save(target);
    assertThat(target.getEmail()).isEqualTo("old@example.com");
    assertThat(target.getUsername()).isEqualTo("old");
    assertThat(target.getPassword()).isEqualTo("old-password");
    assertThat(target.getBio()).isEqualTo("old bio");
    assertThat(target.getImage()).isEqualTo("old image");
  }
}
