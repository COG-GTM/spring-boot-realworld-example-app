package io.spring.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ArticleMetrics {

  private final Counter articlesCreated;
  private final Counter articlesUpdated;
  private final Counter articlesFavorited;
  private final Counter articlesDeleted;

  public ArticleMetrics(MeterRegistry registry) {
    this.articlesCreated =
        Counter.builder("articles.created")
            .description("Number of articles created")
            .register(registry);
    this.articlesUpdated =
        Counter.builder("articles.updated")
            .description("Number of articles updated")
            .register(registry);
    this.articlesFavorited =
        Counter.builder("articles.favorited")
            .description("Number of article favorite actions")
            .register(registry);
    this.articlesDeleted =
        Counter.builder("articles.deleted")
            .description("Number of articles deleted")
            .register(registry);
  }

  public void recordCreated() {
    articlesCreated.increment();
  }

  public void recordUpdated() {
    articlesUpdated.increment();
  }

  public void recordFavorited() {
    articlesFavorited.increment();
  }

  public void recordDeleted() {
    articlesDeleted.increment();
  }
}
