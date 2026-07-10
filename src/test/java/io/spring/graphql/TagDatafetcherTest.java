package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class TagDatafetcherTest extends DgsGraphQLTestBase {

  @Test
  void should_return_all_tags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring"));

    List<String> tags =
        dgsQueryExecutor.executeAndExtractJsonPath("{ tags }", "data.tags");

    assertEquals(Arrays.asList("java", "spring"), tags);
  }
}
