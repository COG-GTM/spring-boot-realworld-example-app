package io.spring.application.user;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

@Import({
  UserService.class,
  MyBatisUserRepository.class,
  BCryptPasswordEncoder.class,
  ValidationAutoConfiguration.class
})
@TestPropertySource(
    properties = "image.default=https://static.productionready.io/images/smiley-cyrus.jpg")
public class UserServiceTest extends DbTestBase {
  @Autowired private UserService userService;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  public void should_create_user_and_encode_password() {
    RegisterParam param = new RegisterParam("new@example.com", "newuser", "plain-password");

    User created = userService.createUser(param);

    Assertions.assertNotNull(created.getId());
    Assertions.assertEquals("new@example.com", created.getEmail());
    Assertions.assertEquals("newuser", created.getUsername());
    Assertions.assertEquals(
        "https://static.productionready.io/images/smiley-cyrus.jpg", created.getImage());
    Assertions.assertNotEquals("plain-password", created.getPassword());
    Assertions.assertTrue(passwordEncoder.matches("plain-password", created.getPassword()));

    Optional<User> fetched = userRepository.findById(created.getId());
    Assertions.assertTrue(fetched.isPresent());
    User saved = fetched.get();
    Assertions.assertEquals("new@example.com", saved.getEmail());
    Assertions.assertEquals("newuser", saved.getUsername());
    Assertions.assertTrue(passwordEncoder.matches("plain-password", saved.getPassword()));
  }

  @Test
  public void should_update_user_and_persist_changes() {
    User user = new User("origin@example.com", "originuser", "123", "", "");
    userRepository.save(user);

    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("updated@example.com")
            .username("updateduser")
            .bio("updated bio")
            .build();

    userService.updateUser(new UpdateUserCommand(user, param));

    Optional<User> fetched = userRepository.findById(user.getId());
    Assertions.assertTrue(fetched.isPresent());
    User saved = fetched.get();
    Assertions.assertEquals("updated@example.com", saved.getEmail());
    Assertions.assertEquals("updateduser", saved.getUsername());
    Assertions.assertEquals("updated bio", saved.getBio());
  }

  @Test
  public void should_allow_update_user_with_own_unchanged_email_and_username() {
    User user = new User("self@example.com", "selfuser", "123", "", "");
    userRepository.save(user);

    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("self@example.com")
            .username("selfuser")
            .bio("new bio")
            .build();

    userService.updateUser(new UpdateUserCommand(user, param));

    Optional<User> fetched = userRepository.findById(user.getId());
    Assertions.assertTrue(fetched.isPresent());
    Assertions.assertEquals("new bio", fetched.get().getBio());
  }

  @Test
  public void should_reject_update_when_email_used_by_another_user() {
    User target = new User("target@example.com", "targetuser", "123", "", "");
    userRepository.save(target);
    User other = new User("other@example.com", "otheruser", "123", "", "");
    userRepository.save(other);

    UpdateUserParam param =
        UpdateUserParam.builder().email("other@example.com").username("targetuser").build();

    Assertions.assertThrows(
        ConstraintViolationException.class,
        () -> userService.updateUser(new UpdateUserCommand(target, param)));
  }

  @Test
  public void should_reject_update_when_username_used_by_another_user() {
    User target = new User("target2@example.com", "targetuser2", "123", "", "");
    userRepository.save(target);
    User other = new User("other2@example.com", "otheruser2", "123", "", "");
    userRepository.save(other);

    UpdateUserParam param =
        UpdateUserParam.builder().email("target2@example.com").username("otheruser2").build();

    Assertions.assertThrows(
        ConstraintViolationException.class,
        () -> userService.updateUser(new UpdateUserCommand(target, param)));
  }
}
