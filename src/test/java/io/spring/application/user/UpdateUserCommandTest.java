package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.core.user.User;
import org.junit.jupiter.api.Test;

public class UpdateUserCommandTest {

  @Test
  public void should_create_update_user_command() {
    User user = new User("test@test.com", "testuser", "pass", "bio", "img");
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("new@test.com")
            .username("newuser")
            .build();

    UpdateUserCommand command = new UpdateUserCommand(user, param);

    assertEquals(user, command.getTargetUser());
    assertEquals(param, command.getParam());
    assertEquals("new@test.com", command.getParam().getEmail());
    assertEquals("newuser", command.getParam().getUsername());
  }

  @Test
  public void should_get_target_user() {
    User user = new User("test@test.com", "testuser", "pass", "bio", "img");
    UpdateUserParam param = new UpdateUserParam();

    UpdateUserCommand command = new UpdateUserCommand(user, param);

    assertNotNull(command.getTargetUser());
    assertEquals(user.getId(), command.getTargetUser().getId());
  }

  @Test
  public void should_get_param() {
    User user = new User("test@test.com", "testuser", "pass", "bio", "img");
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("a@b.com")
            .password("newpass")
            .bio("new bio")
            .image("new img")
            .build();

    UpdateUserCommand command = new UpdateUserCommand(user, param);

    assertNotNull(command.getParam());
    assertEquals("a@b.com", command.getParam().getEmail());
    assertEquals("newpass", command.getParam().getPassword());
    assertEquals("new bio", command.getParam().getBio());
    assertEquals("new img", command.getParam().getImage());
  }
}
