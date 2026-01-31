package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      TagDatafetcher.class
    })
@TestPropertySource(properties = "dgs.graphql.schema-locations=classpath*:schema/**/*.graphqls")
public class TagDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private TagsQueryService tagsQueryService;

  @Test
  public void should_query_all_tags() {
    List<String> tags = Arrays.asList("java", "spring", "graphql", "dgs");
    when(tagsQueryService.allTags()).thenReturn(tags);

    String query = "query { " + "  tags " + "}";

    ExecutionResult result = dgsQueryExecutor.execute(query);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    List<String> tagsResult = (List<String>) data.get("tags");
    assertNotNull(tagsResult);
    assertEquals(4, tagsResult.size());
    assertTrue(tagsResult.contains("java"));
    assertTrue(tagsResult.contains("spring"));
    assertTrue(tagsResult.contains("graphql"));
    assertTrue(tagsResult.contains("dgs"));
  }

  @Test
  public void should_return_empty_list_when_no_tags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList());

    String query = "query { " + "  tags " + "}";

    ExecutionResult result = dgsQueryExecutor.execute(query);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    List<String> tagsResult = (List<String>) data.get("tags");
    assertNotNull(tagsResult);
    assertEquals(0, tagsResult.size());
  }
}
