package io.spring.graphql;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@AutoConfigureDataJpa
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite::memory:",
      "jwt.secret=test-secret-key-for-testing-only",
      "jwt.sessionTime=3600"
    })
public abstract class GraphQLTestBase {
  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @Autowired private JwtService jwtService;

  @Autowired private UserRepository userRepository;

  protected String getJwtToken(User user) {
    return "Token " + jwtService.toToken(user);
  }

  protected DgsQueryExecutor dgsQueryExecutor() {
    return dgsQueryExecutor;
  }
}
