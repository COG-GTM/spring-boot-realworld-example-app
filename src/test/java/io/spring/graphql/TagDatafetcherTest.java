package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

  @Mock private TagsQueryService tagsQueryService;

  @InjectMocks private TagDatafetcher tagDatafetcher;

  @Test
  public void should_get_all_tags() {
    List<String> tags = Arrays.asList("tag1", "tag2", "tag3");
    when(tagsQueryService.allTags()).thenReturn(tags);

    List<String> result = tagDatafetcher.getTags();

    assertNotNull(result);
    assertEquals(3, result.size());
    assertEquals("tag1", result.get(0));
    assertEquals("tag2", result.get(1));
    assertEquals("tag3", result.get(2));
  }

  @Test
  public void should_return_empty_list_when_no_tags() {
    List<String> tags = Arrays.asList();
    when(tagsQueryService.allTags()).thenReturn(tags);

    List<String> result = tagDatafetcher.getTags();

    assertNotNull(result);
    assertEquals(0, result.size());
  }
}
