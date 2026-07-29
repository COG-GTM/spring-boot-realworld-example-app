package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagDatafetcherTest extends GraphqlTestBase {

  @Mock private TagsQueryService tagsQueryService;

  @InjectMocks private TagDatafetcher tagDatafetcher;

  @Test
  void should_return_all_tags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring"));

    assertThat(tagDatafetcher.getTags()).containsExactly("java", "spring");
  }

  @Test
  void should_return_empty_list_when_no_tag_exists() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    assertThat(tagDatafetcher.getTags()).isEmpty();
  }
}
