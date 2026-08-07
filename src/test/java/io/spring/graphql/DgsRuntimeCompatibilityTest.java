package io.spring.graphql;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import io.spring.graphql.types.Article;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
public class DgsRuntimeCompatibilityTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @Test
  public void schema_is_assembled_and_a_query_is_executed() {
    ExecutionResult result = dgsQueryExecutor.execute("{ tags }");

    assertThat(result.getErrors(), is(empty()));
    Map<String, Object> data = result.getData();
    assertThat(data.get("tags"), is(instanceOf(List.class)));
  }

  @Test
  public void codegen_types_are_loadable_at_runtime() {
    Article article = Article.newBuilder().slug("a-slug").title("A title").build();

    assertThat(article.getSlug(), is("a-slug"));
    assertThat(article.getTitle(), is("A title"));
  }
}
