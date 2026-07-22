package io.spring.graphql;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TagDatafetcherTest {

  @Mock private TagsQueryService tagsQueryService;

  private TagDatafetcher tagDatafetcher;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    tagDatafetcher = new TagDatafetcher(tagsQueryService);
  }

  @Test
  public void should_return_all_tags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring"));
    List<String> tags = tagDatafetcher.getTags();
    assertThat(tags, is(Arrays.asList("java", "spring")));
  }
}
