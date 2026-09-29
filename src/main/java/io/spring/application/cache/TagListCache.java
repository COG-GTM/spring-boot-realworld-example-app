package io.spring.application.cache;

import io.spring.core.article.ArticleChangedEvent;
import io.spring.infrastructure.mybatis.readservice.TagReadService;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TagListCache {
  private static final String ALL_TAGS = "all";

  private final TagReadService tagReadService;
  private final GuardedCache<String, List<String>> cache;

  public TagListCache(
      TagReadService tagReadService,
      @Value("${realworld.cache.enabled:true}") boolean enabled,
      @Value("${realworld.cache.ttl:10m}") Duration ttl) {
    this.tagReadService = tagReadService;
    this.cache = new GuardedCache<>(enabled, 1, ttl);
  }

  public List<String> allTags() {
    return cache.get(ALL_TAGS, () -> List.copyOf(tagReadService.all()));
  }

  @EventListener
  public void onArticleChanged(ArticleChangedEvent event) {
    TransactionalEviction.evict(() -> cache.invalidate(Collections.singleton(ALL_TAGS)));
  }
}
