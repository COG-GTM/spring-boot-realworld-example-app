package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import io.spring.application.article.ArticleCommandService;
import io.spring.core.user.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
public class UnauthenticatedMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;

  @MockBean private ArticleCommandService articleCommandService;

  @AfterEach
  public void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_reject_follow_user_when_unauthenticated() {
    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { followUser(username: \"x\") { profile { username } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getExtensions().get("errorType"))
        .isEqualTo("UNAUTHENTICATED");
    Map<String, Object> data = result.getData();
    assertThat(data.get("followUser")).isNull();
    verifyNoInteractions(userRepository);
  }

  @Test
  public void should_reject_create_article_when_unauthenticated() {
    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { createArticle(input: {title: \"t\", description: \"d\", body: \"b\"}) { article { slug } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getExtensions().get("errorType"))
        .isEqualTo("UNAUTHENTICATED");
    Map<String, Object> data = result.getData();
    assertThat(data.get("createArticle")).isNull();
    verifyNoInteractions(articleCommandService);
  }
}
