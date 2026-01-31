package io.spring.application.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @Mock private UserRepository userRepository;

  private PasswordEncoder passwordEncoder;

  private UserService userService;

  private String defaultImage;

  @BeforeEach
  public void setUp() {
    passwordEncoder = new BCryptPasswordEncoder();
    defaultImage = "https://static.productionready.io/images/smiley-cyrus.jpg";
    userService = new UserService(userRepository, defaultImage, passwordEncoder);
  }

  @Test
  public void should_create_user_with_valid_params() {
    RegisterParam param = new RegisterParam("test@example.com", "testuser", "password123");

    User user = userService.createUser(param);

    assertThat(user, notNullValue());
    assertThat(user.getEmail(), is("test@example.com"));
    assertThat(user.getUsername(), is("testuser"));
    assertThat(user.getImage(), is(defaultImage));
    verify(userRepository).save(any(User.class));
  }

  @Test
  public void should_encode_password_when_creating_user() {
    String rawPassword = "password123";
    RegisterParam param = new RegisterParam("test@example.com", "testuser", rawPassword);

    User user = userService.createUser(param);

    assertThat(user.getPassword(), not(rawPassword));
    assertThat(passwordEncoder.matches(rawPassword, user.getPassword()), is(true));
  }

  @Test
  public void should_set_default_image_for_new_user() {
    RegisterParam param = new RegisterParam("test@example.com", "testuser", "password");

    User user = userService.createUser(param);

    assertThat(user.getImage(), is(defaultImage));
  }

  @Test
  public void should_set_empty_bio_for_new_user() {
    RegisterParam param = new RegisterParam("test@example.com", "testuser", "password");

    User user = userService.createUser(param);

    assertThat(user.getBio(), is(""));
  }

  @Test
  public void should_generate_unique_id_for_new_user() {
    RegisterParam param = new RegisterParam("test@example.com", "testuser", "password");

    User user = userService.createUser(param);

    assertThat(user.getId(), notNullValue());
  }

  @Test
  public void should_save_user_to_repository() {
    RegisterParam param = new RegisterParam("test@example.com", "testuser", "password");

    userService.createUser(param);

    verify(userRepository).save(any(User.class));
  }

  @Test
  public void should_update_user_with_valid_command() {
    User existingUser = new User("old@example.com", "olduser", "oldpass", "old bio", "old.jpg");
    UpdateUserParam updateParam =
        new UpdateUserParam("new@example.com", "newpass", "newuser", "new bio", "new.jpg");
    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);

    userService.updateUser(command);

    assertThat(existingUser.getEmail(), is("new@example.com"));
    assertThat(existingUser.getUsername(), is("newuser"));
    assertThat(existingUser.getBio(), is("new bio"));
    assertThat(existingUser.getImage(), is("new.jpg"));
    verify(userRepository).save(existingUser);
  }

  @Test
  public void should_update_only_email() {
    User existingUser =
        new User("old@example.com", "originaluser", "originalpass", "original bio", "original.jpg");
    UpdateUserParam updateParam = new UpdateUserParam("new@example.com", "", "", "", "");
    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);

    userService.updateUser(command);

    assertThat(existingUser.getEmail(), is("new@example.com"));
    assertThat(existingUser.getUsername(), is("originaluser"));
    assertThat(existingUser.getBio(), is("original bio"));
    assertThat(existingUser.getImage(), is("original.jpg"));
  }

  @Test
  public void should_update_only_username() {
    User existingUser =
        new User("original@example.com", "olduser", "originalpass", "original bio", "original.jpg");
    UpdateUserParam updateParam = new UpdateUserParam("", "", "newuser", "", "");
    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);

    userService.updateUser(command);

    assertThat(existingUser.getEmail(), is("original@example.com"));
    assertThat(existingUser.getUsername(), is("newuser"));
  }

  @Test
  public void should_update_only_bio() {
    User existingUser =
        new User("original@example.com", "originaluser", "originalpass", "old bio", "original.jpg");
    UpdateUserParam updateParam = new UpdateUserParam("", "", "", "new bio", "");
    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);

    userService.updateUser(command);

    assertThat(existingUser.getBio(), is("new bio"));
    assertThat(existingUser.getEmail(), is("original@example.com"));
    assertThat(existingUser.getUsername(), is("originaluser"));
  }

  @Test
  public void should_update_only_image() {
    User existingUser =
        new User("original@example.com", "originaluser", "originalpass", "original bio", "old.jpg");
    UpdateUserParam updateParam = new UpdateUserParam("", "", "", "", "new.jpg");
    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);

    userService.updateUser(command);

    assertThat(existingUser.getImage(), is("new.jpg"));
    assertThat(existingUser.getEmail(), is("original@example.com"));
  }

  @Test
  public void should_not_update_any_field_when_all_empty() {
    User existingUser =
        new User("original@example.com", "originaluser", "originalpass", "original bio", "original.jpg");
    UpdateUserParam updateParam = new UpdateUserParam("", "", "", "", "");
    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);

    userService.updateUser(command);

    assertThat(existingUser.getEmail(), is("original@example.com"));
    assertThat(existingUser.getUsername(), is("originaluser"));
    assertThat(existingUser.getBio(), is("original bio"));
    assertThat(existingUser.getImage(), is("original.jpg"));
  }

  @Test
  public void should_save_updated_user_to_repository() {
    User existingUser = new User("old@example.com", "olduser", "oldpass", "old bio", "old.jpg");
    UpdateUserParam updateParam = new UpdateUserParam("new@example.com", "", "", "", "");
    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);

    userService.updateUser(command);

    verify(userRepository).save(existingUser);
  }

  @Test
  public void should_handle_password_update() {
    User existingUser = new User("email@example.com", "user", "oldpass", "bio", "image.jpg");
    UpdateUserParam updateParam = new UpdateUserParam("", "newpassword", "", "", "");
    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);

    userService.updateUser(command);

    assertThat(existingUser.getPassword(), is("newpassword"));
  }
}
