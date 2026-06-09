package io.spring.graphql;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.UserQueryService;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class MeDatafetcherTest {

  @Mock private UserQueryService userQueryService;
  @Mock private JwtService jwtService;

  private MeDatafetcher meDatafetcher;

  @BeforeEach
  public void setUp() {
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
  }

  @AfterEach
  public void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_return_null_when_anonymous() {
    AnonymousAuthenticationToken anonymous =
        new AnonymousAuthenticationToken(
            "key",
            "anonymous",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymous);

    assertThat(meDatafetcher.getMe("Bearer token", null), is(nullValue()));
  }
}
