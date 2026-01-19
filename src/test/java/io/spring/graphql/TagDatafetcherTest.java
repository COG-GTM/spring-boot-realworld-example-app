package io.spring.graphql;

import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {DgsAutoConfiguration.class, TagDatafetcher.class})
public class TagDatafetcherTest {

  @Autowired
  private DgsQueryExecutor dgsQueryExecutor;

  @MockBean
  private TagsQueryService tagsQueryService;

  @Test
  void shouldReturnAllTags() {
    List<String> expectedTags = Arrays.asList("java", "spring", "graphql", "dgs");
    when(tagsQueryService.allTags()).thenReturn(expectedTags);

    List<String> tags = dgsQueryExecutor.executeAndExtractJsonPath(
        "{ tags }",
        "data.tags"
    );

    assertThat(tags).containsExactlyInAnyOrderElementsOf(expectedTags);
  }

  @Test
  void shouldReturnEmptyListWhenNoTags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList());

    List<String> tags = dgsQueryExecutor.executeAndExtractJsonPath(
        "{ tags }",
        "data.tags"
    );

    assertThat(tags).isEmpty();
  }
}
