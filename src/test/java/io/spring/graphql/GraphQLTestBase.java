package io.spring.graphql;

import io.spring.core.user.User;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Shared helpers for GraphQL data fetcher / mutation unit tests that rely on the security context.
 */
abstract class GraphQLTestBase {

  protected void setCurrentUser(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  protected void setAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @AfterEach
  public void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }
}
