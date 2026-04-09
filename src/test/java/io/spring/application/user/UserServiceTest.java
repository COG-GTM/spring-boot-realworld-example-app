package io.spring.application.user;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Import({
  UserService.class,
  MyBatisUserRepository.class,
  BCryptPasswordEncoder.class,
  ValidationAutoConfiguration.class
})
public class UserServiceTest extends DbTestBase {

  @Autowired private UserService userService;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private User existingUser;

  @BeforeEach
  public void setUp() {
    existingUser = new User("existing@test.com", "existing", "123", "bio", "image");
    userRepository.save(existingUser);
  }

  @Test
  public void should_create_user_success() {
    RegisterParam param = new RegisterParam("new@test.com", "newuser", "password");
    User user = userService.createUser(param);

    Assertions.assertNotNull(user.getId());
    Assertions.assertEquals("new@test.com", user.getEmail());
    Assertions.assertEquals("newuser", user.getUsername());
    Assertions.assertTrue(passwordEncoder.matches("password", user.getPassword()));

    Assertions.assertTrue(userRepository.findByEmail("new@test.com").isPresent());
  }

  @Test
  public void should_throw_error_for_blank_email() {
    RegisterParam param = new RegisterParam("", "user1", "password");
    Assertions.assertThrows(
        ConstraintViolationException.class, () -> userService.createUser(param));
  }

  @Test
  public void should_throw_error_for_invalid_email() {
    RegisterParam param = new RegisterParam("not-an-email", "user1", "password");
    Assertions.assertThrows(
        ConstraintViolationException.class, () -> userService.createUser(param));
  }

  @Test
  public void should_throw_error_for_blank_username() {
    RegisterParam param = new RegisterParam("valid@test.com", "", "password");
    Assertions.assertThrows(
        ConstraintViolationException.class, () -> userService.createUser(param));
  }

  @Test
  public void should_throw_error_for_blank_password() {
    RegisterParam param = new RegisterParam("valid@test.com", "user1", "");
    Assertions.assertThrows(
        ConstraintViolationException.class, () -> userService.createUser(param));
  }

  @Test
  public void should_throw_error_for_duplicated_email() {
    RegisterParam param = new RegisterParam("existing@test.com", "uniqueuser", "password");
    Assertions.assertThrows(
        ConstraintViolationException.class, () -> userService.createUser(param));
  }

  @Test
  public void should_throw_error_for_duplicated_username() {
    RegisterParam param = new RegisterParam("unique@test.com", "existing", "password");
    Assertions.assertThrows(
        ConstraintViolationException.class, () -> userService.createUser(param));
  }

  @Test
  public void should_update_user_success() {
    UpdateUserParam updateParam =
        UpdateUserParam.builder()
            .email("updated@test.com")
            .username("updateduser")
            .bio("new bio")
            .image("new image")
            .build();
    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);
    userService.updateUser(command);

    User updated = userRepository.findByEmail("updated@test.com").get();
    Assertions.assertEquals("updateduser", updated.getUsername());
    Assertions.assertEquals("new bio", updated.getBio());
    Assertions.assertEquals("new image", updated.getImage());
  }

  @Test
  public void should_throw_error_when_updating_to_existing_email() {
    User anotherUser = new User("another@test.com", "another", "123", "", "");
    userRepository.save(anotherUser);

    UpdateUserParam updateParam =
        UpdateUserParam.builder().email("another@test.com").username("existing").build();
    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);

    Assertions.assertThrows(
        ConstraintViolationException.class, () -> userService.updateUser(command));
  }

  @Test
  public void should_throw_error_when_updating_to_existing_username() {
    User anotherUser = new User("another@test.com", "another", "123", "", "");
    userRepository.save(anotherUser);

    UpdateUserParam updateParam =
        UpdateUserParam.builder().email("existing@test.com").username("another").build();
    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);

    Assertions.assertThrows(
        ConstraintViolationException.class, () -> userService.updateUser(command));
  }

  @Test
  public void should_allow_update_with_same_email_and_username() {
    UpdateUserParam updateParam =
        UpdateUserParam.builder()
            .email("existing@test.com")
            .username("existing")
            .bio("updated bio")
            .build();
    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);
    userService.updateUser(command);

    User updated = userRepository.findByUsername("existing").get();
    Assertions.assertEquals("updated bio", updated.getBio());
  }
}
