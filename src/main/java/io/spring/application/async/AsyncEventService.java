package io.spring.application.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncEventService {

  private static final Logger logger = LoggerFactory.getLogger(AsyncEventService.class);

  private final CacheManager cacheManager;

  public AsyncEventService(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @Async
  public void evictArticleCache(String articleId) {
    try {
      var cache = cacheManager.getCache("articles");
      if (cache != null) {
        cache.evict(articleId);
        logger.debug("Evicted cache for article: {}", articleId);
      }
    } catch (Exception e) {
      logger.warn("Failed to evict cache for article {}: {}", articleId, e.getMessage());
    }
  }

  @Async
  public void evictArticleCacheBySlug(String slug) {
    try {
      var cache = cacheManager.getCache("articles");
      if (cache != null) {
        cache.evict(slug);
        logger.debug("Evicted cache for article slug: {}", slug);
      }
    } catch (Exception e) {
      logger.warn("Failed to evict cache for article slug {}: {}", slug, e.getMessage());
    }
  }

  @Async
  public void logArticleView(String articleId, String userId) {
    logger.info("Article viewed: articleId={}, userId={}", articleId, userId);
  }

  @Async
  public void logUserActivity(String userId, String action, String resourceType, String resourceId) {
    logger.info(
        "User activity: userId={}, action={}, resourceType={}, resourceId={}",
        userId,
        action,
        resourceType,
        resourceId);
  }
}
