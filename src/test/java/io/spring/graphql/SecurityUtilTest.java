package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.core.user.User;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_return_current_user_when_authenticated_with_user_principal() {
    User user = new User("user@example.com", "username", "123", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, null));

    Optional<User> result = SecurityUtil.getCurrentUser();

    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(user);
  }

  @Test
  void should_return_empty_when_authentication_is_anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    assertThat(SecurityUtil.getCurrentUser()).isEmpty();
  }

  @Test
  void should_return_empty_when_principal_is_null() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(null, null));

    assertThat(SecurityUtil.getCurrentUser()).isEmpty();
  }
}
