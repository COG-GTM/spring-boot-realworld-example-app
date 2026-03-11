package io.spring.application.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import io.spring.core.user.User;
import org.junit.jupiter.api.Test;

public class UpdateUserCommandTest {

  @Test
  public void should_create_update_user_command() {
    User user = mock(User.class);
    UpdateUserParam param = new UpdateUserParam("test@example.com", "pass", "user", "bio", "img");

    UpdateUserCommand command = new UpdateUserCommand(user, param);

    assertThat(command.getTargetUser(), is(user));
    assertThat(command.getParam(), is(param));
  }
}
