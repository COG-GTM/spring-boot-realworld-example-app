package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Import({MyBatisUserRepository.class, UserService.class, BCryptPasswordEncoder.class})
public class UserServicePasswordUpdateTest extends DbTestBase {
  @Autowired private UserService userService;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  public void should_persist_hashed_password_when_updating_password() {
    String email = "john@jacob.com";
    String oldPassword = "old-password";
    String newPassword = "new-password";
    User user = userService.createUser(new RegisterParam(email, "johnjacob", oldPassword));

    userService.updateUser(
        new UpdateUserCommand(user, UpdateUserParam.builder().password(newPassword).build()));

    User persisted = userRepository.findByEmail(email).get();
    assertNotSame(user, persisted);
    assertNotEquals(newPassword, persisted.getPassword());
    assertTrue(passwordEncoder.matches(newPassword, persisted.getPassword()));
    assertFalse(passwordEncoder.matches(oldPassword, persisted.getPassword()));
  }
}
