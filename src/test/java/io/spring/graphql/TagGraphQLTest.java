package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {DgsAutoConfiguration.class, TagDatafetcher.class})
@ActiveProfiles("test")
public class TagGraphQLTest extends GraphQLTestBase {

  @MockBean private TagsQueryService tagsQueryService;

  @Test
  void should_query_all_tags() {
    List<String> expectedTags = Arrays.asList("java", "spring", "graphql", "testing");

    when(tagsQueryService.allTags()).thenReturn(expectedTags);

    String query = "query { tags }";

    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assertThat(result).hasSize(4);
    assertThat(result).containsExactlyInAnyOrder("java", "spring", "graphql", "testing");
  }

  @Test
  void should_return_empty_list_when_no_tags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList());

    String query = "query { tags }";

    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assertThat(result).isEmpty();
  }
}
