package io.spring.graphql;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TagDatafetcherTest extends GraphQLTestBase {

  @Test
  public void should_return_all_tags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring"));

    List<String> tags =
        dgsQueryExecutor.executeAndExtractJsonPath("{ tags }", "data.tags");

    Assertions.assertEquals(Arrays.asList("java", "spring"), tags);
  }

  @Test
  public void should_return_empty_list_when_no_tags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList());

    List<String> tags =
        dgsQueryExecutor.executeAndExtractJsonPath("{ tags }", "data.tags");

    Assertions.assertTrue(tags.isEmpty());
  }
}
