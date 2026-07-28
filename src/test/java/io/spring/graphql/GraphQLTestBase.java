package io.spring.graphql;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.spring.core.user.User;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

abstract class GraphQLTestBase {

  protected User user;

  @BeforeEach
  void setUpCurrentUser() {
    user =
        new User(
            "john@jacob.com",
            "johnjacob",
            "123",
            "bio",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    authenticate(user);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  protected void authenticate(User currentUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                currentUser, null, AuthorityUtils.createAuthorityList("ROLE_USER")));
  }

  protected void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  protected DataFetchingEnvironment dfe(Object source, Object localContext) {
    return dfe(source, localContext, Collections.emptyMap());
  }

  protected DataFetchingEnvironment dfe(
      Object source, Object localContext, Map<String, Object> arguments) {
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
        .source(source)
        .localContext(localContext)
        .arguments(arguments)
        .build();
  }

  protected DgsDataFetchingEnvironment dgsDfe(Object source, Object localContext) {
    return new DgsDataFetchingEnvironment(dfe(source, localContext));
  }
}
