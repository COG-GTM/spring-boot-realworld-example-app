package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TagDatafetcherTest {

  private TagsQueryService tagsQueryService;
  private TagDatafetcher tagDatafetcher;

  @BeforeEach
  void setUp() {
    tagsQueryService = mock(TagsQueryService.class);
    tagDatafetcher = new TagDatafetcher(tagsQueryService);
  }

  @Test
  void should_return_tags_from_query_service() {
    List<String> tags = Arrays.asList("java", "spring");
    when(tagsQueryService.allTags()).thenReturn(tags);

    List<String> result = tagDatafetcher.getTags();

    assertEquals(tags, result);
    verify(tagsQueryService).allTags();
  }

  @Test
  void should_return_empty_list_when_no_tags() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    List<String> result = tagDatafetcher.getTags();

    assertTrue(result.isEmpty());
    verify(tagsQueryService).allTags();
  }
}
