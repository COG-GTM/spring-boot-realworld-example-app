package io.spring.graphql;

import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtilTest {

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_return_current_user_when_authenticated() {
    User user = new User("email@example.com", "username", "123", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    Optional<User> currentUser = SecurityUtil.getCurrentUser();
    Assertions.assertTrue(currentUser.isPresent());
    Assertions.assertEquals(user, currentUser.get());
  }

  @Test
  public void should_return_empty_for_anonymous_authentication() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    Assertions.assertFalse(SecurityUtil.getCurrentUser().isPresent());
  }
}
