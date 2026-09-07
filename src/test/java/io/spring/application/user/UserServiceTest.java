package io.spring.application.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserServiceTest {

  @Test
  public void should_create_user() {
    UserRepository userRepository = mock(UserRepository.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    when(passwordEncoder.encode("password")).thenReturn("encoded");
    UserService userService =
        new UserService(userRepository, "http://default.png", passwordEncoder);

    User user = userService.createUser(new RegisterParam("email", "username", "password"));

    assertThat(user.getEmail(), is("email"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("encoded"));
    assertThat(user.getBio(), is(""));
    assertThat(user.getImage(), is("http://default.png"));
    verify(userRepository).save(user);
  }

  @Test
  public void should_update_user() {
    UserRepository userRepository = mock(UserRepository.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    UserService userService =
        new UserService(userRepository, "http://default.png", passwordEncoder);
    User targetUser = new User("old@email", "old-user", "old-password", "old-bio", "old-image");

    userService.updateUser(
        new UpdateUserCommand(
            targetUser,
            new UpdateUserParam("new@email", "new-password", "new-user", "new-bio", "new-image")));

    assertThat(targetUser.getEmail(), is("new@email"));
    assertThat(targetUser.getUsername(), is("new-user"));
    assertThat(targetUser.getPassword(), is("new-password"));
    assertThat(targetUser.getBio(), is("new-bio"));
    assertThat(targetUser.getImage(), is("new-image"));
    verify(userRepository).save(targetUser);
  }
}
