package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class TagDatafetcherTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private TagsQueryService tagsQueryService;

  @Test
  public void should_get_all_tags() {
    List<String> tags = Arrays.asList("java", "spring", "graphql", "testing");
    when(tagsQueryService.allTags()).thenReturn(tags);

    String query = "query { tags }";

    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assertThat(result).isNotNull();
    assertThat(result).hasSize(4);
    assertThat(result).containsExactlyInAnyOrder("java", "spring", "graphql", "testing");
  }

  @Test
  public void should_return_empty_list_when_no_tags() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    String query = "query { tags }";

    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }

  @Test
  public void should_get_tags_without_authentication() {
    clearAuthentication();

    List<String> tags = Arrays.asList("java", "spring");
    when(tagsQueryService.allTags()).thenReturn(tags);

    String query = "query { tags }";

    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
  }
}
