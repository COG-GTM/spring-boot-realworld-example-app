package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {DgsAutoConfiguration.class, TagDatafetcher.class})
public class TagDatafetcherTest {

  @Autowired DgsQueryExecutor dgsQueryExecutor;

  @MockBean TagsQueryService tagsQueryService;

  @Test
  public void testGetTags() {
    List<String> tags = Arrays.asList("java", "spring", "graphql", "dgs");
    when(tagsQueryService.allTags()).thenReturn(tags);

    String query = "{ tags }";
    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assertNotNull(result);
    assertEquals(4, result.size());
    assertTrue(result.contains("java"));
    assertTrue(result.contains("spring"));
    assertTrue(result.contains("graphql"));
    assertTrue(result.contains("dgs"));
  }

  @Test
  public void testGetTagsEmpty() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    String query = "{ tags }";
    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  public void testGetTagsSingleTag() {
    List<String> tags = Arrays.asList("java");
    when(tagsQueryService.allTags()).thenReturn(tags);

    String query = "{ tags }";
    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("java", result.get(0));
  }
}
