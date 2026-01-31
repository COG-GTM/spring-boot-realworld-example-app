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
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {DgsAutoConfiguration.class, TagDatafetcher.class})
@ActiveProfiles("test")
public class TagDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private TagsQueryService tagsQueryService;

  @Test
  void shouldGetAllTags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring", "graphql", "dgs"));

    String query = "{ tags }";

    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assertThat(result).containsExactly("java", "spring", "graphql", "dgs");
  }

  @Test
  void shouldReturnEmptyListWhenNoTags() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    String query = "{ tags }";

    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnSingleTag() {
    when(tagsQueryService.allTags()).thenReturn(Collections.singletonList("java"));

    String query = "{ tags }";

    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assertThat(result).containsExactly("java");
  }
}
