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

public class TagDatafetcherTest {

  private TagsQueryService tagsQueryService;
  private TagDatafetcher tagDatafetcher;

  @BeforeEach
  void setUp() {
    tagsQueryService = mock(TagsQueryService.class);
    tagDatafetcher = new TagDatafetcher(tagsQueryService);
  }

  @Test
  void getTags_returns_all_tags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring"));

    List<String> tags = tagDatafetcher.getTags();

    assertEquals(Arrays.asList("java", "spring"), tags);
    verify(tagsQueryService).allTags();
  }

  @Test
  void getTags_returns_empty_list_when_no_tags() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    List<String> tags = tagDatafetcher.getTags();

    assertTrue(tags.isEmpty());
    verify(tagsQueryService).allTags();
  }
}
