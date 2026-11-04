package io.spring.graphql;

import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtilTest {

  @AfterEach
  public void tearDown() {
    GraphQLTestSupport.clear();
  }

  @Test
  public void should_return_current_user_when_authenticated() {
    User user = new User("a@test.com", "a", "123", "", "");
    GraphQLTestSupport.authenticate(user);

    Optional<User> current = SecurityUtil.getCurrentUser();

    Assertions.assertTrue(current.isPresent());
    Assertions.assertSame(user, current.get());
  }

  @Test
  public void should_return_empty_for_anonymous_user() {
    GraphQLTestSupport.anonymous();

    Assertions.assertFalse(SecurityUtil.getCurrentUser().isPresent());
  }

  @Test
  public void should_return_empty_when_principal_is_null() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(null, null));

    Assertions.assertFalse(SecurityUtil.getCurrentUser().isPresent());
  }
}
