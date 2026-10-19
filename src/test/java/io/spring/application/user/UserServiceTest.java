package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class UserServiceTest {
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private UserRepository userRepository;
  private UserService userService;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    userService = new UserService(userRepository, "default", passwordEncoder);
  }

  @Test
  public void should_hash_password_on_update() {
    User user = new User("a@b.com", "user", passwordEncoder.encode("oldpw"), "", "");

    userService.updateUser(
        new UpdateUserCommand(user, UpdateUserParam.builder().password("newpw").build()));

    assertNotEquals("newpw", user.getPassword());
    assertTrue(passwordEncoder.matches("newpw", user.getPassword()));
    verify(userRepository).save(user);
  }

  @Test
  public void should_keep_password_when_update_has_no_password() {
    String oldHash = passwordEncoder.encode("oldpw");
    User user = new User("a@b.com", "user", oldHash, "", "");

    userService.updateUser(
        new UpdateUserCommand(user, UpdateUserParam.builder().bio("bio").build()));

    assertEquals(oldHash, user.getPassword());
  }
}
