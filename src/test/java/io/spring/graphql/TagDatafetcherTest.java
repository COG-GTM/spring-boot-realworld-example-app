package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
public class TagDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private TagsQueryService tagsQueryService;

  @Test
  public void should_get_all_tags() {
    List<String> tags = Arrays.asList("java", "spring", "graphql", "dgs");
    when(tagsQueryService.allTags()).thenReturn(tags);

    String query = "query { tags }";

    List<String> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");
    assertNotNull(result);
    assertEquals(4, result.size());
    assertTrue(result.contains("java"));
    assertTrue(result.contains("spring"));
    assertTrue(result.contains("graphql"));
    assertTrue(result.contains("dgs"));
  }

  @Test
  public void should_return_empty_list_when_no_tags() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    String query = "query { tags }";

    List<String> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void should_return_single_tag() {
    List<String> tags = Arrays.asList("java");
    when(tagsQueryService.allTags()).thenReturn(tags);

    String query = "query { tags }";

    List<String> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("java", result.get(0));
  }

  @Test
  public void should_handle_tags_with_special_characters() {
    List<String> tags = Arrays.asList("c++", "c#", "node.js", "vue-js");
    when(tagsQueryService.allTags()).thenReturn(tags);

    String query = "query { tags }";

    List<String> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");
    assertNotNull(result);
    assertEquals(4, result.size());
    assertTrue(result.contains("c++"));
    assertTrue(result.contains("c#"));
    assertTrue(result.contains("node.js"));
    assertTrue(result.contains("vue-js"));
  }
}
