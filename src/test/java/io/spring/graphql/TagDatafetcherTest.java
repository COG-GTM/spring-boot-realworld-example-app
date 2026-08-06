package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TagDatafetcherTest {

  @Test
  void should_return_all_tags() {
    TagsQueryService tagsQueryService = mock(TagsQueryService.class);
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring"));

    TagDatafetcher datafetcher = new TagDatafetcher(tagsQueryService);
    List<String> tags = datafetcher.getTags();

    assertEquals(Arrays.asList("java", "spring"), tags);
  }
}
