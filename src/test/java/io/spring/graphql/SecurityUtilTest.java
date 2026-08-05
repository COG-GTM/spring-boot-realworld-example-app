package io.spring.graphql;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtilTest {

  private final User user = new User("john@example.com", "john", "123", "bio", "image");

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_get_current_user_from_security_context() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                user, null, AuthorityUtils.createAuthorityList("ROLE_USER")));

    Optional<User> currentUser = SecurityUtil.getCurrentUser();

    assertTrue(currentUser.isPresent());
    assertThat(currentUser.get(), is(user));
  }

  @Test
  public void should_get_empty_with_anonymous_authentication() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertFalse(SecurityUtil.getCurrentUser().isPresent());
  }

  @Test
  public void should_get_empty_with_null_principal() {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(null);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    assertFalse(SecurityUtil.getCurrentUser().isPresent());
  }

  @Test
  public void should_throw_null_pointer_without_authentication() {
    SecurityContextHolder.clearContext();

    assertThrows(NullPointerException.class, SecurityUtil::getCurrentUser);
  }

  @Test
  public void should_throw_class_cast_exception_with_unexpected_principal_type() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "not-a-user", null, AuthorityUtils.createAuthorityList("ROLE_USER")));

    assertThrows(ClassCastException.class, SecurityUtil::getCurrentUser);
  }
}
