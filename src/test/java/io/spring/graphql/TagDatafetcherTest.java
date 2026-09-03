package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

@SpringBootTest(classes = {DgsAutoConfiguration.class, TagDatafetcher.class})
public class TagDatafetcherTest {
  @MockBean private TagsQueryService tagsQueryService;

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @Test
  public void should_get_all_tags() {
    List<String> tags = Arrays.asList("java", "spring");
    when(tagsQueryService.allTags()).thenReturn(tags);

    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath("{ tags }", "data.tags");

    assertEquals(tags, result);
  }
}
