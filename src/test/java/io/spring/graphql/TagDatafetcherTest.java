package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TagDatafetcherTest {
  @Mock private TagsQueryService tagsQueryService;

  @Test
  public void should_return_all_tags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring"));
    TagDatafetcher datafetcher = new TagDatafetcher(tagsQueryService);
    assertThat(datafetcher.getTags()).containsExactly("java", "spring");
  }

  @Test
  public void should_return_empty_list_when_no_tags() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());
    TagDatafetcher datafetcher = new TagDatafetcher(tagsQueryService);
    assertThat(datafetcher.getTags()).isEmpty();
  }
}
