package io.spring.application.user;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Import({UserService.class, MyBatisUserRepository.class, BCryptPasswordEncoder.class})
public class UserServiceTest extends DbTestBase {
  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  public void should_create_user_successfully() {
    RegisterParam registerParam = new RegisterParam("test@test.com", "testuser", "password123");

    User user = userService.createUser(registerParam);

    Assertions.assertNotNull(user);
    Assertions.assertNotNull(user.getId());
    Assertions.assertEquals("test@test.com", user.getEmail());
    Assertions.assertEquals("testuser", user.getUsername());
    Assertions.assertTrue(passwordEncoder.matches("password123", user.getPassword()));
  }

  @Test
  public void should_save_user_to_repository() {
    RegisterParam registerParam = new RegisterParam("save@test.com", "saveuser", "password");

    User createdUser = userService.createUser(registerParam);

    Optional<User> foundUser = userRepository.findById(createdUser.getId());
    Assertions.assertTrue(foundUser.isPresent());
    Assertions.assertEquals(createdUser.getId(), foundUser.get().getId());
    Assertions.assertEquals("save@test.com", foundUser.get().getEmail());
  }

  @Test
  public void should_update_user_email() {
    RegisterParam registerParam = new RegisterParam("original@test.com", "originaluser", "password");
    User user = userService.createUser(registerParam);

    UpdateUserParam updateParam = UpdateUserParam.builder().email("updated@test.com").build();
    UpdateUserCommand command = new UpdateUserCommand(user, updateParam);

    userService.updateUser(command);

    Optional<User> updatedUser = userRepository.findById(user.getId());
    Assertions.assertTrue(updatedUser.isPresent());
    Assertions.assertEquals("updated@test.com", updatedUser.get().getEmail());
  }

  @Test
  public void should_update_user_username() {
    RegisterParam registerParam = new RegisterParam("user@test.com", "oldusername", "password");
    User user = userService.createUser(registerParam);

    UpdateUserParam updateParam = UpdateUserParam.builder().username("newusername").build();
    UpdateUserCommand command = new UpdateUserCommand(user, updateParam);

    userService.updateUser(command);

    Optional<User> updatedUser = userRepository.findById(user.getId());
    Assertions.assertTrue(updatedUser.isPresent());
    Assertions.assertEquals("newusername", updatedUser.get().getUsername());
  }

  @Test
  public void should_update_user_bio() {
    RegisterParam registerParam = new RegisterParam("bio@test.com", "biouser", "password");
    User user = userService.createUser(registerParam);

    UpdateUserParam updateParam = UpdateUserParam.builder().bio("This is my new bio").build();
    UpdateUserCommand command = new UpdateUserCommand(user, updateParam);

    userService.updateUser(command);

    Optional<User> updatedUser = userRepository.findById(user.getId());
    Assertions.assertTrue(updatedUser.isPresent());
    Assertions.assertEquals("This is my new bio", updatedUser.get().getBio());
  }

  @Test
  public void should_update_user_image() {
    RegisterParam registerParam = new RegisterParam("image@test.com", "imageuser", "password");
    User user = userService.createUser(registerParam);

    UpdateUserParam updateParam = UpdateUserParam.builder().image("http://new-image.url").build();
    UpdateUserCommand command = new UpdateUserCommand(user, updateParam);

    userService.updateUser(command);

    Optional<User> updatedUser = userRepository.findById(user.getId());
    Assertions.assertTrue(updatedUser.isPresent());
    Assertions.assertEquals("http://new-image.url", updatedUser.get().getImage());
  }

  @Test
  public void should_update_multiple_fields() {
    RegisterParam registerParam = new RegisterParam("multi@test.com", "multiuser", "password");
    User user = userService.createUser(registerParam);

    UpdateUserParam updateParam =
        UpdateUserParam.builder()
            .email("newmulti@test.com")
            .username("newmultiuser")
            .bio("New bio")
            .image("http://new-image.url")
            .build();
    UpdateUserCommand command = new UpdateUserCommand(user, updateParam);

    userService.updateUser(command);

    Optional<User> updatedUser = userRepository.findById(user.getId());
    Assertions.assertTrue(updatedUser.isPresent());
    Assertions.assertEquals("newmulti@test.com", updatedUser.get().getEmail());
    Assertions.assertEquals("newmultiuser", updatedUser.get().getUsername());
    Assertions.assertEquals("New bio", updatedUser.get().getBio());
    Assertions.assertEquals("http://new-image.url", updatedUser.get().getImage());
  }

  @Test
  public void should_create_multiple_users() {
    RegisterParam param1 = new RegisterParam("user1@test.com", "user1", "password1");
    RegisterParam param2 = new RegisterParam("user2@test.com", "user2", "password2");

    User user1 = userService.createUser(param1);
    User user2 = userService.createUser(param2);

    Assertions.assertNotEquals(user1.getId(), user2.getId());
    Assertions.assertEquals("user1@test.com", user1.getEmail());
    Assertions.assertEquals("user2@test.com", user2.getEmail());
  }
}
