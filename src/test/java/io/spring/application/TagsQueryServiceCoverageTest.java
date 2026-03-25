package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.spring.infrastructure.mybatis.readservice.TagReadService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TagsQueryServiceCoverageTest {

  private TagReadService tagReadService;
  private TagsQueryService tagsQueryService;

  @BeforeEach
  void setUp() {
    tagReadService = mock(TagReadService.class);
    tagsQueryService = new TagsQueryService(tagReadService);
  }

  @Test
  public void should_return_all_tags() {
    when(tagReadService.all()).thenReturn(Arrays.asList("java", "spring", "kotlin"));

    List<String> result = tagsQueryService.allTags();

    assertNotNull(result);
    assertEquals(3, result.size());
    assertTrue(result.contains("java"));
  }

  @Test
  public void should_return_empty_tags() {
    when(tagReadService.all()).thenReturn(Collections.emptyList());

    List<String> result = tagsQueryService.allTags();

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }
}
