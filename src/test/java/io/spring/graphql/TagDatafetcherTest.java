package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TagDatafetcherTest {

  @Mock
  private TagsQueryService tagsQueryService;

  @InjectMocks
  private TagDatafetcher tagDatafetcher;

  @Test
  void shouldGetAllTags() {
    List<String> tags = Arrays.asList("java", "spring", "graphql", "dgs");
    when(tagsQueryService.allTags()).thenReturn(tags);

    List<String> result = tagDatafetcher.getTags();

    assertThat(result).containsExactlyInAnyOrder("java", "spring", "graphql", "dgs");
  }

  @Test
  void shouldReturnEmptyTagsList() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList());

    List<String> result = tagDatafetcher.getTags();

    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnSingleTag() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java"));

    List<String> result = tagDatafetcher.getTags();

    assertThat(result).containsExactly("java");
  }
}
