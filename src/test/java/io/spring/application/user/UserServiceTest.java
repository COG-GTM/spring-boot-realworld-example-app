package io.spring.application.user;

import static org.hamcrest.CoreMatchers.is;
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
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, "default_image", passwordEncoder);
  }

  @Test
  public void should_create_user_with_encoded_password_and_default_image() {
    when(passwordEncoder.encode("123")).thenReturn("encoded");

    User user = userService.createUser(new RegisterParam("email@test.com", "username", "123"));

    assertThat(user.getEmail(), is("email@test.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("encoded"));
    assertThat(user.getImage(), is("default_image"));
    assertThat(user.getBio(), is(""));
    verify(userRepository).save(any(User.class));
  }

  @Test
  public void should_update_target_user_and_save_it() {
    User user = new User("email@test.com", "username", "123", "bio", "image");
    UpdateUserParam param = UpdateUserParam.builder().email("new@test.com").bio("new bio").build();

    userService.updateUser(new UpdateUserCommand(user, param));

    assertThat(user.getEmail(), is("new@test.com"));
    assertThat(user.getBio(), is("new bio"));
    assertThat(user.getUsername(), is("username"));
    verify(userRepository).save(user);
  }
}
