package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
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

  @Test
  public void should_create_user_with_encoded_password_and_default_image() {
    UserService service = new UserService(userRepository, DEFAULT_IMAGE, passwordEncoder);
    when(passwordEncoder.encode("123")).thenReturn("encoded");

    User user = service.createUser(new RegisterParam("john@example.com", "john", "123"));

    assertThat(user.getEmail()).isEqualTo("john@example.com");
    assertThat(user.getUsername()).isEqualTo("john");
    assertThat(user.getPassword()).isEqualTo("encoded");
    assertThat(user.getBio()).isEmpty();
    assertThat(user.getImage()).isEqualTo(DEFAULT_IMAGE);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertThat(captor.getValue()).isEqualTo(user);
  }

  @Test
  public void should_update_target_user_and_save_it() {
    UserService service = new UserService(userRepository, DEFAULT_IMAGE, passwordEncoder);
    User targetUser = new User("john@example.com", "john", "123", "bio", "image");
    UpdateUserParam param =
        UpdateUserParam.builder().email("new@example.com").bio("new bio").build();

    service.updateUser(new UpdateUserCommand(targetUser, param));

    assertThat(targetUser.getEmail()).isEqualTo("new@example.com");
    assertThat(targetUser.getBio()).isEqualTo("new bio");
    assertThat(targetUser.getUsername()).isEqualTo("john");
    assertThat(targetUser.getPassword()).isEqualTo("123");
    assertThat(targetUser.getImage()).isEqualTo("image");
    verify(userRepository).save(targetUser);
  }
}
