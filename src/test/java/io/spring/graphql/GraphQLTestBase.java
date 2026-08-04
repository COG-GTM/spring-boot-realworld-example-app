package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.ExecutionResult;
import io.spring.core.user.User;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Shared security context handling for the DGS datafetcher and mutation tests. */
public abstract class GraphQLTestBase {

  @BeforeEach
  void clearSecurityContextBeforeTest() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void clearSecurityContextAfterTest() {
    SecurityContextHolder.clearContext();
  }

  protected void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  /**
   * Asserts the query produced exactly one error and that it was raised by the given exception,
   * rather than by an unrelated validation or parse failure in the query document.
   */
  protected void assertSingleErrorFrom(
      ExecutionResult result, Class<? extends Throwable> exceptionType) {
    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains(exceptionType.getName());
  }

  /** Mimics what Spring Security puts in the context for an unauthenticated request. */
  protected void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }
}
