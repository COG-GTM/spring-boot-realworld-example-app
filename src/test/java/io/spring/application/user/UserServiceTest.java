package io.spring.application.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private UserService userService;
  private final String defaultImage = "default-image";

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    userService = new UserService(userRepository, defaultImage, passwordEncoder);
  }

  @Test
  public void should_create_user_with_encoded_password_and_default_image() {
    RegisterParam registerParam = new RegisterParam("email@test.com", "username", "password");
    when(passwordEncoder.encode(eq("password"))).thenReturn("encoded");

    User user = userService.createUser(registerParam);

    assertThat(user.getEmail(), is("email@test.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("encoded"));
    assertThat(user.getImage(), is(defaultImage));
    assertThat(user.getBio(), is(""));
    verify(passwordEncoder).encode(eq("password"));
    verify(userRepository).save(any(User.class));
  }

  @Test
  public void should_update_target_user_and_save() {
    User targetUser = new User("old@test.com", "oldname", "pass", "oldbio", "oldimage");
    UpdateUserParam param =
        UpdateUserParam.builder().email("new@test.com").username("newname").bio("newbio").build();

    userService.updateUser(new UpdateUserCommand(targetUser, param));

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.getEmail(), is("new@test.com"));
    assertThat(saved.getUsername(), is("newname"));
    assertThat(saved.getBio(), is("newbio"));
    assertThat(saved.getImage(), is("oldimage"));
  }
}
