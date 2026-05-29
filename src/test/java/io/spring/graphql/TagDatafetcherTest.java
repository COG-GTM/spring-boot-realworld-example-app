package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TagDatafetcherTest {

  @Test
  void should_return_all_tags() {
    TagsQueryService tagsQueryService = mock(TagsQueryService.class);
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring", "graphql"));

    TagDatafetcher fetcher = new TagDatafetcher(tagsQueryService);
    List<String> tags = fetcher.getTags();

    assertEquals(3, tags.size());
    assertTrue(tags.contains("java"));
    assertTrue(tags.contains("spring"));
    assertTrue(tags.contains("graphql"));
  }

  @Test
  void should_return_empty_list_when_no_tags() {
    TagsQueryService tagsQueryService = mock(TagsQueryService.class);
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList());

    TagDatafetcher fetcher = new TagDatafetcher(tagsQueryService);
    List<String> tags = fetcher.getTags();

    assertTrue(tags.isEmpty());
  }
}
