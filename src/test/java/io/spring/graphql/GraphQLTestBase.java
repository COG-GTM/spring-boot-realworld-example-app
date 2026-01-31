package io.spring.graphql;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public abstract class GraphQLTestBase {

  @Autowired protected DgsQueryExecutor dgsQueryExecutor;

  @BeforeEach
  public void setUpSecurityContext() {
    AnonymousAuthenticationToken anonymousToken =
        new AnonymousAuthenticationToken(
            "anonymous",
            "anonymousUser",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);
  }
}
