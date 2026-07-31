package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SecurityUtilTest extends GraphQLTestBase {

  @Test
  void should_return_the_authenticated_user() {
    User user = userFixture("john");
    authenticate(user);

    Optional<User> current = SecurityUtil.getCurrentUser();

    assertThat(current).contains(user);
  }

  @Test
  void should_return_empty_for_anonymous_authentication() {
    anonymous();

    assertThat(SecurityUtil.getCurrentUser()).isEmpty();
  }
}
