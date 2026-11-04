package io.spring.graphql;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.spring.core.user.User;
import java.util.Collections;
import java.util.Map;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

final class GraphQLTestSupport {
  private GraphQLTestSupport() {}

  static void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  static void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  static void clear() {
    SecurityContextHolder.clearContext();
  }

  static DataFetchingEnvironment env(Object source, Object localContext) {
    return env(source, localContext, Collections.emptyMap());
  }

  static DataFetchingEnvironment env(
      Object source, Object localContext, Map<String, Object> arguments) {
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
        .source(source)
        .localContext(localContext)
        .arguments(arguments)
        .build();
  }

  static DgsDataFetchingEnvironment dgsEnv(Object source, Object localContext) {
    return new DgsDataFetchingEnvironment(env(source, localContext));
  }
}
