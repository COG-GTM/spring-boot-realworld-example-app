package io.spring.graphql;

import io.spring.core.user.User;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Shared security-context plumbing for the DGS datafetcher tests. */
public abstract class DgsTestBase {

  protected void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                user, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))));
  }

  protected void authenticateAnonymously() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "anonymous",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  protected HttpHeaders authorizationHeader(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token " + token);
    return headers;
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }
}
