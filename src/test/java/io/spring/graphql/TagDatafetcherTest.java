package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TagDatafetcherTest {
  private TagsQueryService tagsQueryService;
  private TagDatafetcher tagDatafetcher;

  @BeforeEach
  public void setUp() {
    tagsQueryService = mock(TagsQueryService.class);
    tagDatafetcher = new TagDatafetcher(tagsQueryService);
  }

  @Test
  public void should_return_all_tags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring"));

    assertThat(tagDatafetcher.getTags()).containsExactly("java", "spring");
    verify(tagsQueryService).allTags();
  }

  @Test
  public void should_return_empty_list_when_no_tags() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    assertThat(tagDatafetcher.getTags()).isEmpty();
  }
}
