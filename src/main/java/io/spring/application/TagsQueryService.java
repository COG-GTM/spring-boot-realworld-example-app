package io.spring.application;

import io.spring.application.cache.TagListCache;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TagsQueryService {
  private TagListCache tagListCache;

  public List<String> allTags() {
    return tagListCache.allTags();
  }
}
