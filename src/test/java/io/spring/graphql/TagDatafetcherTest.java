package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagDatafetcherTest {

  @Mock private TagsQueryService tagsQueryService;

  private TagDatafetcher tagDatafetcher;

  @BeforeEach
  void setUp() {
    tagDatafetcher = new TagDatafetcher(tagsQueryService);
  }

  @Test
  void getTags_returnsTags() {
    List<String> tags = Arrays.asList("java", "spring", "graphql");
    when(tagsQueryService.allTags()).thenReturn(tags);

    List<String> result = tagDatafetcher.getTags();

    assertNotNull(result);
    assertEquals(3, result.size());
    assertEquals(Arrays.asList("java", "spring", "graphql"), result);
  }

  @Test
  void getTags_emptyList_returnsEmptyList() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    List<String> result = tagDatafetcher.getTags();

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void getTags_singleTag_returnsSingleElementList() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("only-tag"));

    List<String> result = tagDatafetcher.getTags();

    assertEquals(1, result.size());
    assertEquals("only-tag", result.get(0));
  }

  @Test
  void getTags_delegatesToService() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    tagDatafetcher.getTags();

    verify(tagsQueryService).allTags();
  }
}
