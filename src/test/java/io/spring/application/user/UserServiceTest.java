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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

@Import({UserService.class, MyBatisUserRepository.class, UserServiceTest.TestConfig.class})
@TestPropertySource(properties = "image.default=https://static.productionready.io/images/smiley-cyrus.jpg")
public class UserServiceTest extends DbTestBase {

  static class TestConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }
  }

  @Autowired private UserService userService;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private User existingUser;

  @BeforeEach
  public void setUp() {
    existingUser = new User("existing@test.com", "existinguser", "password123", "", "");
    userRepository.save(existingUser);
  }

  @Test
  public void should_create_user_successfully() {
    RegisterParam param = new RegisterParam("new@test.com", "newuser", "password");
    User user = userService.createUser(param);

    Assertions.assertNotNull(user.getId());
    Assertions.assertEquals("new@test.com", user.getEmail());
    Assertions.assertEquals("newuser", user.getUsername());
    Assertions.assertTrue(passwordEncoder.matches("password", user.getPassword()));
    Assertions.assertEquals(
        "https://static.productionready.io/images/smiley-cyrus.jpg", user.getImage());

    Optional<User> saved = userRepository.findById(user.getId());
    Assertions.assertTrue(saved.isPresent());
    Assertions.assertEquals("new@test.com", saved.get().getEmail());
  }

  @Test
  public void should_create_user_with_default_bio() {
    RegisterParam param = new RegisterParam("bio@test.com", "biouser", "password");
    User user = userService.createUser(param);

    Assertions.assertEquals("", user.getBio());
  }

  @Test
  public void should_update_user_successfully() {
    UpdateUserParam updateParam =
        UpdateUserParam.builder()
            .email("updated@test.com")
            .username("updateduser")
            .bio("new bio")
            .image("new image")
            .build();

    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);
    userService.updateUser(command);

    User updated = userRepository.findById(existingUser.getId()).get();
    Assertions.assertEquals("updated@test.com", updated.getEmail());
    Assertions.assertEquals("updateduser", updated.getUsername());
    Assertions.assertEquals("new bio", updated.getBio());
    Assertions.assertEquals("new image", updated.getImage());
  }

  @Test
  public void should_update_user_email_only() {
    UpdateUserParam updateParam =
        UpdateUserParam.builder().email("newemail@test.com").build();

    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);
    userService.updateUser(command);

    User updated = userRepository.findById(existingUser.getId()).get();
    Assertions.assertEquals("newemail@test.com", updated.getEmail());
    Assertions.assertEquals("existinguser", updated.getUsername());
  }

  @Test
  public void should_update_user_password() {
    UpdateUserParam updateParam =
        UpdateUserParam.builder().password("newpassword").build();

    UpdateUserCommand command = new UpdateUserCommand(existingUser, updateParam);
    userService.updateUser(command);

    User updated = userRepository.findById(existingUser.getId()).get();
    Assertions.assertEquals("newpassword", updated.getPassword());
  }
}
