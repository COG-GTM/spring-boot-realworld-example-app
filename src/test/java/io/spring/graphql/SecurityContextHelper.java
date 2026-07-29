package io.spring.graphql;

import io.spring.core.user.User;
import java.util.Collections;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/** Helpers to drive the {@link SecurityContextHolder} state the graphql components read from. */
final class SecurityContextHelper {

  private SecurityContextHelper() {}

  static void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  static void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  static void authenticateWithNullPrincipal() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(null, null));
  }

  static void clear() {
    SecurityContextHolder.clearContext();
  }
}
