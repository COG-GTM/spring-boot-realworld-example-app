package io.spring.graphql;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
  public void setUp() {
    tagsQueryService = mock(TagsQueryService.class);
    tagDatafetcher = new TagDatafetcher(tagsQueryService);
  }

  @Test
  public void should_get_all_tags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring", "graphql"));

    List<String> tags = tagDatafetcher.getTags();

    assertThat(tags, is(Arrays.asList("java", "spring", "graphql")));
    verify(tagsQueryService, times(1)).allTags();
  }

  @Test
  public void should_get_empty_list_when_no_tag_exists() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    assertThat(tagDatafetcher.getTags().isEmpty(), is(true));
  }

  @Test
  public void should_query_tags_on_every_call() {
    when(tagsQueryService.allTags())
        .thenReturn(Collections.singletonList("java"))
        .thenReturn(Arrays.asList("java", "spring"));

    assertThat(tagDatafetcher.getTags(), is(Collections.singletonList("java")));
    assertThat(tagDatafetcher.getTags(), is(Arrays.asList("java", "spring")));
    verify(tagsQueryService, times(2)).allTags();
  }
}
