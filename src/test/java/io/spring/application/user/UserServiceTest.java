package io.spring.application.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserServiceTest {

  private static final String DEFAULT_IMAGE = "https://static.productionready.io/images/smiley.jpg";

  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private UserService userService;

  @BeforeEach
  public void setUp() {
    userRepository = Mockito.mock(UserRepository.class);
    passwordEncoder = Mockito.mock(PasswordEncoder.class);
    userService = new UserService(userRepository, DEFAULT_IMAGE, passwordEncoder);
  }

  @Test
  public void should_save_user_with_encoded_password_and_default_image() {
    when(passwordEncoder.encode("123")).thenReturn("encoded-123");

    User created = userService.createUser(new RegisterParam("john@example.com", "john", "123"));

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();

    assertThat(saved, is(created));
    assertThat(saved.getId(), notNullValue());
    assertThat(saved.getEmail(), is("john@example.com"));
    assertThat(saved.getUsername(), is("john"));
    assertThat(saved.getPassword(), is("encoded-123"));
    assertThat(saved.getBio(), is(""));
    assertThat(saved.getImage(), is(DEFAULT_IMAGE));
    verify(passwordEncoder).encode("123");
  }

  @Test
  public void should_never_save_raw_password() {
    when(passwordEncoder.encode(Mockito.anyString())).thenReturn("encoded");

    User created =
        userService.createUser(new RegisterParam("jane@example.com", "jane", "raw-password"));

    assertThat(created.getPassword(), is("encoded"));
    verify(passwordEncoder).encode(eq("raw-password"));
  }

  @Test
  public void should_create_users_with_distinct_ids() {
    when(passwordEncoder.encode(Mockito.anyString())).thenReturn("encoded");

    User first = userService.createUser(new RegisterParam("a@example.com", "a", "123"));
    User second = userService.createUser(new RegisterParam("b@example.com", "b", "123"));

    assertThat(first.getId().equals(second.getId()), is(false));
    verify(userRepository, times(2)).save(Mockito.any(User.class));
  }

  @Test
  public void should_update_all_user_fields_and_save() {
    User targetUser = new User("old@example.com", "old", "old-password", "old bio", "old image");
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("new@example.com")
            .username("new")
            .password("new-password")
            .bio("new bio")
            .image("new image")
            .build();

    userService.updateUser(new UpdateUserCommand(targetUser, param));

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();

    assertThat(saved, is(targetUser));
    assertThat(saved.getEmail(), is("new@example.com"));
    assertThat(saved.getUsername(), is("new"));
    assertThat(saved.getPassword(), is("new-password"));
    assertThat(saved.getBio(), is("new bio"));
    assertThat(saved.getImage(), is("new image"));
  }

  @Test
  public void should_only_update_provided_fields() {
    User targetUser = new User("old@example.com", "old", "old-password", "old bio", "old image");
    UpdateUserParam param = UpdateUserParam.builder().email("new@example.com").build();

    userService.updateUser(new UpdateUserCommand(targetUser, param));

    verify(userRepository).save(targetUser);
    assertThat(targetUser.getEmail(), is("new@example.com"));
    assertThat(targetUser.getUsername(), is("old"));
    assertThat(targetUser.getPassword(), is("old-password"));
    assertThat(targetUser.getBio(), is("old bio"));
    assertThat(targetUser.getImage(), is("old image"));
  }

  @Test
  public void should_keep_all_fields_when_update_param_is_empty() {
    User targetUser = new User("old@example.com", "old", "old-password", "old bio", "old image");

    userService.updateUser(new UpdateUserCommand(targetUser, UpdateUserParam.builder().build()));

    verify(userRepository).save(targetUser);
    assertThat(targetUser.getEmail(), is("old@example.com"));
    assertThat(targetUser.getUsername(), is("old"));
    assertThat(targetUser.getPassword(), is("old-password"));
    assertThat(targetUser.getBio(), is("old bio"));
    assertThat(targetUser.getImage(), is("old image"));
  }

  @Test
  public void should_ignore_null_fields_on_update() {
    User targetUser = new User("old@example.com", "old", "old-password", "old bio", "old image");
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email(null)
            .password(null)
            .username("new")
            .bio(null)
            .image(null)
            .build();

    userService.updateUser(new UpdateUserCommand(targetUser, param));

    verify(userRepository).save(targetUser);
    assertThat(targetUser.getEmail(), is("old@example.com"));
    assertThat(targetUser.getUsername(), is("new"));
    assertThat(targetUser.getPassword(), is("old-password"));
    assertThat(targetUser.getBio(), is("old bio"));
    assertThat(targetUser.getImage(), is("old image"));
  }

  /**
   * Pins current behavior: unlike {@code createUser}, {@code updateUser} stores the new password
   * without running it through the {@link PasswordEncoder}. If that is fixed in main source, this
   * test must be updated.
   */
  @Test
  public void should_currently_store_update_password_unencoded() {
    User targetUser = new User("old@example.com", "old", "old-password", "old bio", "old image");
    UpdateUserParam param = UpdateUserParam.builder().password("plain-new-password").build();

    userService.updateUser(new UpdateUserCommand(targetUser, param));

    assertThat(targetUser.getPassword(), is("plain-new-password"));
    verifyNoInteractions(passwordEncoder);
    verify(userRepository).save(targetUser);
    verify(userRepository, never()).findByEmail(Mockito.anyString());
  }
}
