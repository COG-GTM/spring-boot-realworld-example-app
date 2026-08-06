package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.TestHelper;
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
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_return_the_authenticated_user() {
    User user = TestHelper.userFixture("current");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    Optional<User> currentUser = SecurityUtil.getCurrentUser();

    assertThat(currentUser).contains(user);
  }

  @Test
  public void should_return_empty_for_anonymous_authentication() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    assertThat(SecurityUtil.getCurrentUser()).isEmpty();
  }

  @Test
  public void should_return_empty_when_principal_is_null() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(null, null));

    assertThat(SecurityUtil.getCurrentUser()).isEmpty();
  }
}
