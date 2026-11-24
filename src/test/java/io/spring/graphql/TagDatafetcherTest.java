package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TagDatafetcherTest extends GraphQLTestBase {

  @Test
  public void should_query_tags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring"));

    List<String> tags = dgsQueryExecutor.executeAndExtractJsonPath("{ tags }", "data.tags");

    assertThat(tags).containsExactly("java", "spring");
  }
}
