package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.core.user.User;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilTest extends GraphQLTestBase {

  @Test
  void should_return_the_authenticated_principal() {
    User user = new User("me@example.com", "me", "123", "", "");
    authenticate(user);

    assertThat(SecurityUtil.getCurrentUser()).contains(user);
  }

  @Test
  void should_return_empty_for_anonymous_authentication() {
    anonymous();

    assertThat(SecurityUtil.getCurrentUser()).isEmpty();
  }

  @Test
  void should_return_empty_when_principal_is_missing() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(null, null, Collections.emptyList()));

    Optional<User> currentUser = SecurityUtil.getCurrentUser();

    assertThat(currentUser).isEmpty();
  }
}
