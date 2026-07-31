package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.TagsQueryService;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      GraphQLCustomizeExceptionHandler.class,
      TagDatafetcher.class
    })
class TagDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private TagsQueryService tagsQueryService;

  @Test
  void should_return_all_tags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "graphql"));

    List<String> tags = dgsQueryExecutor.executeAndExtractJsonPath("{ tags }", "data.tags");

    assertThat(tags).containsExactly("java", "graphql");
  }

  @Test
  void should_return_empty_list_when_no_tag_exists() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    List<String> tags = dgsQueryExecutor.executeAndExtractJsonPath("{ tags }", "data.tags");

    assertThat(tags).isEmpty();
  }
}
