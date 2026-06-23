package io.spring.graphql;

import io.spring.core.user.User;
import java.util.Collections;
import java.util.List;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Test helper for wiring the Spring SecurityContext that {@code SecurityUtil} reads from. */
final class GraphQLTestSecurity {

  private GraphQLTestSecurity() {}

  /** Authenticate as the given user so {@code SecurityUtil.getCurrentUser()} resolves to it. */
  static void login(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  /** Set an anonymous token so {@code SecurityUtil.getCurrentUser()} returns empty. */
  static void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  static void clear() {
    SecurityContextHolder.clearContext();
  }
}
