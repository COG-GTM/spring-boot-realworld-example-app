package io.spring.application.user;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Import({
  UserService.class,
  MyBatisUserRepository.class,
  BCryptPasswordEncoder.class
})
public class UserServiceTest extends DbTestBase {

  @Autowired private UserService userService;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  public void setUp() {
  }

  @Test
  public void should_create_user_with_valid_params() {
    RegisterParam param = new RegisterParam("test@example.com", "testuser", "password123");

    User user = userService.createUser(param);

    Assertions.assertNotNull(user);
    Assertions.assertEquals("test@example.com", user.getEmail());
    Assertions.assertEquals("testuser", user.getUsername());
    Assertions.assertNotNull(user.getId());
    Assertions.assertTrue(passwordEncoder.matches("password123", user.getPassword()));

    Optional<User> savedUser = userRepository.findById(user.getId());
    Assertions.assertTrue(savedUser.isPresent());
    Assertions.assertEquals("testuser", savedUser.get().getUsername());
  }

  @Test
  public void should_create_user_with_encoded_password() {
    RegisterParam param = new RegisterParam("user@test.com", "username", "mypassword");

    User user = userService.createUser(param);

    Assertions.assertNotEquals("mypassword", user.getPassword());
    Assertions.assertTrue(passwordEncoder.matches("mypassword", user.getPassword()));
  }

  @Test
  public void should_create_multiple_users() {
    RegisterParam param1 = new RegisterParam("user1@test.com", "user1", "password1");
    RegisterParam param2 = new RegisterParam("user2@test.com", "user2", "password2");

    User user1 = userService.createUser(param1);
    User user2 = userService.createUser(param2);

    Assertions.assertNotEquals(user1.getId(), user2.getId());
    Assertions.assertEquals("user1", user1.getUsername());
    Assertions.assertEquals("user2", user2.getUsername());
  }

  @Test
  public void should_update_user_email() {
    User user = new User("original@test.com", "testuser", "password", "bio", "image");
    userRepository.save(user);

    UpdateUserParam param = UpdateUserParam.builder()
        .email("updated@test.com")
        .username("testuser")
        .password("")
        .bio("bio")
        .image("image")
        .build();

    userService.updateUser(new UpdateUserCommand(user, param));

    Optional<User> updatedUser = userRepository.findById(user.getId());
    Assertions.assertTrue(updatedUser.isPresent());
    Assertions.assertEquals("updated@test.com", updatedUser.get().getEmail());
  }

  @Test
  public void should_update_user_username() {
    User user = new User("test@test.com", "oldusername", "password", "bio", "image");
    userRepository.save(user);

    UpdateUserParam param = UpdateUserParam.builder()
        .email("test@test.com")
        .username("newusername")
        .password("")
        .bio("bio")
        .image("image")
        .build();

    userService.updateUser(new UpdateUserCommand(user, param));

    Optional<User> updatedUser = userRepository.findById(user.getId());
    Assertions.assertTrue(updatedUser.isPresent());
    Assertions.assertEquals("newusername", updatedUser.get().getUsername());
  }

  @Test
  public void should_update_user_bio() {
    User user = new User("test@test.com", "testuser", "password", "old bio", "image");
    userRepository.save(user);

    UpdateUserParam param = UpdateUserParam.builder()
        .email("test@test.com")
        .username("testuser")
        .password("")
        .bio("new bio content")
        .image("image")
        .build();

    userService.updateUser(new UpdateUserCommand(user, param));

    Optional<User> updatedUser = userRepository.findById(user.getId());
    Assertions.assertTrue(updatedUser.isPresent());
    Assertions.assertEquals("new bio content", updatedUser.get().getBio());
  }

  @Test
  public void should_update_user_image() {
    User user = new User("test@test.com", "testuser", "password", "bio", "old-image.jpg");
    userRepository.save(user);

    UpdateUserParam param = UpdateUserParam.builder()
        .email("test@test.com")
        .username("testuser")
        .password("")
        .bio("bio")
        .image("new-image.jpg")
        .build();

    userService.updateUser(new UpdateUserCommand(user, param));

    Optional<User> updatedUser = userRepository.findById(user.getId());
    Assertions.assertTrue(updatedUser.isPresent());
    Assertions.assertEquals("new-image.jpg", updatedUser.get().getImage());
  }

  @Test
  public void should_update_user_password() {
    User user = new User("test@test.com", "testuser", "oldpassword", "bio", "image");
    userRepository.save(user);

    UpdateUserParam param = UpdateUserParam.builder()
        .email("test@test.com")
        .username("testuser")
        .password("newpassword")
        .bio("bio")
        .image("image")
        .build();

    userService.updateUser(new UpdateUserCommand(user, param));

    Optional<User> updatedUser = userRepository.findById(user.getId());
    Assertions.assertTrue(updatedUser.isPresent());
    Assertions.assertEquals("newpassword", updatedUser.get().getPassword());
  }

  @Test
  public void should_update_multiple_user_fields() {
    User user = new User("old@test.com", "olduser", "oldpass", "old bio", "old.jpg");
    userRepository.save(user);

    UpdateUserParam param = UpdateUserParam.builder()
        .email("new@test.com")
        .username("newuser")
        .password("newpass")
        .bio("new bio")
        .image("new.jpg")
        .build();

    userService.updateUser(new UpdateUserCommand(user, param));

    Optional<User> updatedUser = userRepository.findById(user.getId());
    Assertions.assertTrue(updatedUser.isPresent());
    Assertions.assertEquals("new@test.com", updatedUser.get().getEmail());
    Assertions.assertEquals("newuser", updatedUser.get().getUsername());
    Assertions.assertEquals("new bio", updatedUser.get().getBio());
    Assertions.assertEquals("new.jpg", updatedUser.get().getImage());
  }

  @Test
  public void should_preserve_user_id_after_update() {
    User user = new User("test@test.com", "testuser", "password", "bio", "image");
    userRepository.save(user);
    String originalId = user.getId();

    UpdateUserParam param = UpdateUserParam.builder()
        .email("updated@test.com")
        .username("updateduser")
        .password("newpass")
        .bio("new bio")
        .image("new.jpg")
        .build();

    userService.updateUser(new UpdateUserCommand(user, param));

    Optional<User> updatedUser = userRepository.findById(originalId);
    Assertions.assertTrue(updatedUser.isPresent());
    Assertions.assertEquals(originalId, updatedUser.get().getId());
  }
}
