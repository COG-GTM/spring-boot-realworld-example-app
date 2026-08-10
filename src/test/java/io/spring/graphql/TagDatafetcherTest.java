package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class TagDatafetcherTest {

  private final TagsQueryService tagsQueryService = mock(TagsQueryService.class);
  private final TagDatafetcher datafetcher = new TagDatafetcher(tagsQueryService);

  @Test
  void should_return_all_tags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring"));

    List<String> tags = datafetcher.getTags();

    assertThat(tags).containsExactly("java", "spring");
    verify(tagsQueryService).allTags();
  }

  @Test
  void should_return_empty_list_when_no_tags_exist() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    assertThat(datafetcher.getTags()).isEmpty();
  }
}
