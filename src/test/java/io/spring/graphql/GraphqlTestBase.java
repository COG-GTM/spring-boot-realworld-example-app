package io.spring.graphql;

import static org.mockito.Mockito.mock;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironment;
import io.spring.core.user.User;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/** Shared security context and DGS environment plumbing for the graphql layer unit tests. */
abstract class GraphqlTestBase {

  protected DataFetchingEnvironment environment;
  protected DgsDataFetchingEnvironment dgsEnvironment;

  protected GraphqlTestBase() {
    environment = mock(DataFetchingEnvironment.class);
    dgsEnvironment = new DgsDataFetchingEnvironment(environment);
  }

  @SuppressWarnings("unchecked")
  protected static <K, V> Map<K, V> asMap(Object localContext) {
    return (Map<K, V>) localContext;
  }

  protected void login(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  protected void logout() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }
}
