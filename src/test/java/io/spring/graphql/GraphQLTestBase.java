package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.ExecutionResult;
import io.spring.core.user.User;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

abstract class GraphQLTestBase {

  protected static final String DEFAULT_AVATAR =
      "https://static.productionready.io/images/smiley-cyrus.jpg";

  protected User userFixture(String seed) {
    return new User(seed + "@test.com", seed, "123", "bio of " + seed, DEFAULT_AVATAR);
  }

  protected void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  protected void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "anonymous",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  protected void assertFailedWith(ExecutionResult result, Class<? extends Throwable> exception) {
    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains(exception.getName());
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }
}
