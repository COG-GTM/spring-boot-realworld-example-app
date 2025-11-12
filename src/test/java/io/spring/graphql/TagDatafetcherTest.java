package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TagDatafetcherTest {

  @Mock private TagsQueryService tagsQueryService;

  @InjectMocks private TagDatafetcher tagDatafetcher;

  @Test
  public void should_get_all_tags() {
    List<String> tags = Arrays.asList("java", "spring", "graphql", "testing");
    when(tagsQueryService.allTags()).thenReturn(tags);

    List<String> result = tagDatafetcher.getTags();

    assertThat(result).isNotNull();
    assertThat(result).hasSize(4);
    assertThat(result).containsExactly("java", "spring", "graphql", "testing");
  }

  @Test
  public void should_return_empty_list_when_no_tags() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    List<String> result = tagDatafetcher.getTags();

    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }
}
