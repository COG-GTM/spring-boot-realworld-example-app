package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.core.user.User;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtilTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_return_current_user_when_authenticated() {
    User user = new User("e@e.com", "user", "pass", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

    Optional<User> currentUser = SecurityUtil.getCurrentUser();

    assertTrue(currentUser.isPresent());
    assertEquals(user, currentUser.get());
    assertEquals(user.getId(), currentUser.get().getId());
  }

  @Test
  void should_return_empty_when_anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymous",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANON"))));

    Optional<User> currentUser = SecurityUtil.getCurrentUser();

    assertFalse(currentUser.isPresent());
  }
}
