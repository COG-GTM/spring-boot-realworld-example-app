package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtilTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_return_empty_when_anonymous() {
    AnonymousAuthenticationToken auth =
        new AnonymousAuthenticationToken(
            "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(auth);

    Optional<User> result = SecurityUtil.getCurrentUser();

    assertFalse(result.isPresent());
  }

  @Test
  void should_return_user_when_authenticated() {
    User user = new User("test@example.com", "testuser", "password", "", "");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(user, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    Optional<User> result = SecurityUtil.getCurrentUser();

    assertTrue(result.isPresent());
    assertEquals("testuser", result.get().getUsername());
  }

  @Test
  void should_throw_when_no_authentication() {
    SecurityContextHolder.clearContext();

    // SecurityUtil calls authentication.getPrincipal() without null check
    // so it throws NullPointerException when no authentication is set
    assertThrows(NullPointerException.class, () -> SecurityUtil.getCurrentUser());
  }

  @Test
  void should_return_empty_when_principal_is_null() {
    TestingAuthenticationToken auth = new TestingAuthenticationToken(null, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    Optional<User> result = SecurityUtil.getCurrentUser();

    assertFalse(result.isPresent());
  }
}
