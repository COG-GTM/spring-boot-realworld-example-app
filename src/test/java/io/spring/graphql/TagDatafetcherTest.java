package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
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

  @Autowired
  private DgsQueryExecutor dgsQueryExecutor;

  @MockBean
  private TagsQueryService tagsQueryService;

  @Test
  void shouldGetAllTags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring", "graphql", "dgs"));

    String query = "{ tags }";

    List<String> tags = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assertThat(tags).containsExactly("java", "spring", "graphql", "dgs");
  }

  @Test
  void shouldReturnEmptyListWhenNoTags() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    String query = "{ tags }";

    List<String> tags = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assertThat(tags).isEmpty();
  }

  @Test
  void shouldReturnSingleTag() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java"));

    String query = "{ tags }";

    List<String> tags = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assertThat(tags).containsExactly("java");
  }
}
