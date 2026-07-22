package io.spring.graphql;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class SecurityUtilTest extends GraphQLTestBase {

  @Test
  public void should_return_current_user_when_authenticated() {
    User user = new User("email@test.com", "username", "pass", "", "");
    setCurrentUser(user);

    Optional<User> current = SecurityUtil.getCurrentUser();
    assertThat(current.isPresent(), is(true));
    assertThat(current.get(), is(user));
  }

  @Test
  public void should_return_empty_when_anonymous() {
    setAnonymous();
    assertThat(SecurityUtil.getCurrentUser().isPresent(), is(false));
  }
}
