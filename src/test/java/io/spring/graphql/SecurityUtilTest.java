package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.core.user.User;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_return_current_user_when_authenticated() {
    User user = new User("test@example.com", "tester", "123", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

    Optional<User> current = SecurityUtil.getCurrentUser();

    assertTrue(current.isPresent());
    assertEquals(user, current.get());
  }

  @Test
  void should_return_empty_when_anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertFalse(SecurityUtil.getCurrentUser().isPresent());
  }

  @Test
  void should_return_empty_when_principal_is_null() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(null, null));

    assertFalse(SecurityUtil.getCurrentUser().isPresent());
  }

  @Test
  void should_throw_when_no_authentication_present() {
    SecurityContextHolder.clearContext();

    // Documents current behaviour: with no authentication in the context the
    // instanceof check is false for null and getPrincipal() is dereferenced.
    assertThrows(NullPointerException.class, SecurityUtil::getCurrentUser);
  }
}
