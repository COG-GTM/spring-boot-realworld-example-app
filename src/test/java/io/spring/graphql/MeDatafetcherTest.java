package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.user.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
public class MeDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserQueryService userQueryService;

  @AfterEach
  public void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_return_current_user_when_authenticated() {
    User user = new User("a@b.com", "alice", "pw", "", "img");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    when(userQueryService.findById(user.getId()))
        .thenReturn(Optional.of(new UserData(user.getId(), "a@b.com", "alice", "", "img")));

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token abc");

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ me { email username token } }", Map.of(), headers);

    assertThat((String) context.read("data.me.email")).isEqualTo("a@b.com");
    assertThat((String) context.read("data.me.username")).isEqualTo("alice");
    assertThat((String) context.read("data.me.token")).isEqualTo("abc");
  }

  @Test
  public void should_return_null_me_when_unauthenticated() {
    ExecutionResult result = dgsQueryExecutor.execute("{ me { email username token } }");

    assertThat(result.getErrors()).isEmpty();
    Map<String, Object> data = result.getData();
    assertThat(data.get("me")).isNull();
  }
}
