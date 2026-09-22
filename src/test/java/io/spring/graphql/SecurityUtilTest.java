package io.spring.graphql;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtilTest {

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_get_authenticated_user() {
    User user = new User("email@test.com", "username", "123", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, emptyList()));

    Optional<User> currentUser = SecurityUtil.getCurrentUser();

    assertTrue(currentUser.isPresent());
    assertEquals(user, currentUser.get());
  }

  @Test
  public void should_get_empty_for_anonymous_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    assertFalse(SecurityUtil.getCurrentUser().isPresent());
  }
}
