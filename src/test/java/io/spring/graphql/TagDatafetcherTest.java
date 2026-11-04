package io.spring.graphql;

import static org.mockito.Mockito.when;

import io.spring.application.TagsQueryService;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
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
  public void should_return_all_tags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring"));

    Assertions.assertEquals(Arrays.asList("java", "spring"), tagDatafetcher.getTags());
  }
}
