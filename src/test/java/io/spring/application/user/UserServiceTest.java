package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserServiceTest {
  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private UserService userService;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    userService = new UserService(userRepository, "default.png", passwordEncoder);
  }

  @Test
  public void should_create_user_with_encoded_password_and_default_image() {
    when(passwordEncoder.encode(eq("secret"))).thenReturn("encoded");

    User user = userService.createUser(new RegisterParam("a@b.com", "alice", "secret"));

    assertEquals("a@b.com", user.getEmail());
    assertEquals("alice", user.getUsername());
    assertEquals("encoded", user.getPassword());
    assertEquals("default.png", user.getImage());
    assertEquals("", user.getBio());
    verify(userRepository).save(user);
  }

  @Test
  public void should_update_only_provided_fields() {
    User user = new User("a@b.com", "alice", "pass", "bio", "img");
    UpdateUserParam param = UpdateUserParam.builder().username("bob").bio("new bio").build();

    userService.updateUser(new UpdateUserCommand(user, param));

    assertEquals("a@b.com", user.getEmail());
    assertEquals("bob", user.getUsername());
    assertEquals("pass", user.getPassword());
    assertEquals("new bio", user.getBio());
    assertEquals("img", user.getImage());
    verify(userRepository).save(user);
  }
}
