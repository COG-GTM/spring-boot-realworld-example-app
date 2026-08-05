package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
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

  private static final String DEFAULT_IMAGE = "https://static.productionready.io/images/smiley.jpg";

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  private UserService userService;

  @BeforeEach
  public void setUp() {
    userService = new UserService(userRepository, DEFAULT_IMAGE, passwordEncoder);
  }

  @Test
  public void should_create_user_with_encoded_password_and_default_image() {
    when(passwordEncoder.encode("123")).thenReturn("encoded-123");

    User user = userService.createUser(new RegisterParam("aisensiy@gmail.com", "aisensiy", "123"));

    assertThat(user.getEmail()).isEqualTo("aisensiy@gmail.com");
    assertThat(user.getUsername()).isEqualTo("aisensiy");
    assertThat(user.getPassword()).isEqualTo("encoded-123");
    assertThat(user.getBio()).isEmpty();
    assertThat(user.getImage()).isEqualTo(DEFAULT_IMAGE);
    assertThat(user.getId()).isNotEmpty();
  }

  @Test
  public void should_save_created_user_into_repository() {
    when(passwordEncoder.encode("123")).thenReturn("encoded-123");

    User user = userService.createUser(new RegisterParam("aisensiy@gmail.com", "aisensiy", "123"));

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue()).isEqualTo(user);
  }

  @Test
  public void should_update_all_provided_fields_of_target_user() {
    User targetUser = new User("old@email.com", "oldname", "123", "old bio", "old image");
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("new@email.com")
            .username("newname")
            .password("newpassword")
            .bio("new bio")
            .image("new image")
            .build();

    userService.updateUser(new UpdateUserCommand(targetUser, param));

    assertThat(targetUser.getEmail()).isEqualTo("new@email.com");
    assertThat(targetUser.getUsername()).isEqualTo("newname");
    assertThat(targetUser.getPassword()).isEqualTo("newpassword");
    assertThat(targetUser.getBio()).isEqualTo("new bio");
    assertThat(targetUser.getImage()).isEqualTo("new image");
    verify(userRepository, times(1)).save(targetUser);
  }

  @Test
  public void should_keep_original_values_for_empty_update_fields() {
    User targetUser = new User("old@email.com", "oldname", "123", "old bio", "old image");

    userService.updateUser(
        new UpdateUserCommand(targetUser, UpdateUserParam.builder().bio("new bio").build()));

    assertThat(targetUser.getEmail()).isEqualTo("old@email.com");
    assertThat(targetUser.getUsername()).isEqualTo("oldname");
    assertThat(targetUser.getPassword()).isEqualTo("123");
    assertThat(targetUser.getBio()).isEqualTo("new bio");
    assertThat(targetUser.getImage()).isEqualTo("old image");
    verify(userRepository, times(1)).save(targetUser);
  }
}
