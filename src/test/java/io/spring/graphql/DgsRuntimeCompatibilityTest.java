package io.spring.graphql;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.graphql.types.Article;
import java.util.List;
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
    List<String> tags = dgsQueryExecutor.executeAndExtractJsonPath("{ tags }", "data.tags");

    assertThat(tags, is(notNullValue()));
  }

  @Test
  public void codegen_types_are_loadable_at_runtime() {
    Article article = Article.newBuilder().slug("a-slug").title("A title").build();

    assertThat(article.getSlug(), is("a-slug"));
    assertThat(article.getTitle(), is("A title"));
  }
}
