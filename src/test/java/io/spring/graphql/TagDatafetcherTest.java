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
public class TagDatafetcherTest {

  @Mock private TagsQueryService tagsQueryService;

  private TagDatafetcher tagDatafetcher;

  @BeforeEach
  void setUp() {
    tagDatafetcher = new TagDatafetcher(tagsQueryService);
  }

  @Test
  void getTags_returnsListOfTags() {
    List<String> expectedTags = Arrays.asList("java", "spring", "graphql", "testing");
    when(tagsQueryService.allTags()).thenReturn(expectedTags);

    List<String> result = tagDatafetcher.getTags();

    assertNotNull(result);
    assertEquals(4, result.size());
    assertTrue(result.contains("java"));
    assertTrue(result.contains("spring"));
    assertTrue(result.contains("graphql"));
    assertTrue(result.contains("testing"));
    verify(tagsQueryService).allTags();
  }

  @Test
  void getTags_withEmptyList_returnsEmptyList() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    List<String> result = tagDatafetcher.getTags();

    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(tagsQueryService).allTags();
  }

  @Test
  void getTags_withSingleTag_returnsSingleTag() {
    List<String> expectedTags = Arrays.asList("java");
    when(tagsQueryService.allTags()).thenReturn(expectedTags);

    List<String> result = tagDatafetcher.getTags();

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("java", result.get(0));
  }

  @Test
  void getTags_preservesOrder() {
    List<String> expectedTags = Arrays.asList("alpha", "beta", "gamma", "delta");
    when(tagsQueryService.allTags()).thenReturn(expectedTags);

    List<String> result = tagDatafetcher.getTags();

    assertNotNull(result);
    assertEquals(4, result.size());
    assertEquals("alpha", result.get(0));
    assertEquals("beta", result.get(1));
    assertEquals("gamma", result.get(2));
    assertEquals("delta", result.get(3));
  }

  @Test
  void getTags_withDuplicateTags_returnsDuplicates() {
    List<String> expectedTags = Arrays.asList("java", "java", "spring");
    when(tagsQueryService.allTags()).thenReturn(expectedTags);

    List<String> result = tagDatafetcher.getTags();

    assertNotNull(result);
    assertEquals(3, result.size());
  }

  @Test
  void getTags_withSpecialCharacters_returnsTagsWithSpecialCharacters() {
    List<String> expectedTags = Arrays.asList("c++", "c#", "node.js", "vue-js");
    when(tagsQueryService.allTags()).thenReturn(expectedTags);

    List<String> result = tagDatafetcher.getTags();

    assertNotNull(result);
    assertEquals(4, result.size());
    assertTrue(result.contains("c++"));
    assertTrue(result.contains("c#"));
    assertTrue(result.contains("node.js"));
    assertTrue(result.contains("vue-js"));
  }

  @Test
  void getTags_callsServiceOnce() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("tag1"));

    tagDatafetcher.getTags();

    verify(tagsQueryService, times(1)).allTags();
  }

  @Test
  void getTags_withLongTagList_returnsAllTags() {
    List<String> manyTags = Arrays.asList(
        "tag1", "tag2", "tag3", "tag4", "tag5",
        "tag6", "tag7", "tag8", "tag9", "tag10",
        "tag11", "tag12", "tag13", "tag14", "tag15",
        "tag16", "tag17", "tag18", "tag19", "tag20");
    when(tagsQueryService.allTags()).thenReturn(manyTags);

    List<String> result = tagDatafetcher.getTags();

    assertNotNull(result);
    assertEquals(20, result.size());
  }
}
