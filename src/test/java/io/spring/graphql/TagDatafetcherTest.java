package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TagDatafetcherTest {

  @Test
  public void should_get_all_tags() {
    TagsQueryService tagsQueryService = mock(TagsQueryService.class);
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring", "react"));

    TagDatafetcher tagDatafetcher = new TagDatafetcher(tagsQueryService);
    List<String> tags = tagDatafetcher.getTags();

    assertEquals(3, tags.size());
    assertTrue(tags.contains("java"));
    assertTrue(tags.contains("spring"));
    assertTrue(tags.contains("react"));
  }
}
