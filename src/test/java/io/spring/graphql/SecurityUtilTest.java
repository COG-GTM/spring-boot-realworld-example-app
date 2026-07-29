package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.core.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilTest extends GraphqlTestBase {

  @Test
  void should_return_current_user_when_authenticated() {
    User user = new User("john@jacob.com", "johnjacob", "123", "", "");
    login(user);

    assertThat(SecurityUtil.getCurrentUser()).contains(user);
  }

  @Test
  void should_return_empty_when_anonymous() {
    logout();

    assertThat(SecurityUtil.getCurrentUser()).isEmpty();
  }

  @Test
  void should_return_empty_when_principal_is_null() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(null, null));

    assertThat(SecurityUtil.getCurrentUser()).isEmpty();
  }
}
