package io.spring.infrastructure.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.spring.application.TagsQueryService;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import({
  MetricsConfig.class,
  MyBatisMetricsInterceptorTest.RegistryConfig.class,
  TagsQueryService.class,
  MyBatisArticleRepository.class
})
public class MyBatisMetricsInterceptorTest extends DbTestBase {
  private static final String TAG_QUERY =
      "io.spring.infrastructure.mybatis.readservice.TagReadService.all";

  @TestConfiguration
  static class RegistryConfig {
    @Bean
    MeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }
  }

  @Autowired private MeterRegistry registry;
  @Autowired private TagsQueryService tagsQueryService;
  @Autowired private ArticleRepository articleRepository;

  @Test
  public void should_count_and_time_select_statements() {
    tagsQueryService.allTags();
    tagsQueryService.allTags();

    assertEquals(
        2.0,
        registry
            .get(MyBatisMetricsInterceptor.QUERY_COUNT_METRIC)
            .tags("statement", TAG_QUERY, "command", "SELECT", "outcome", "SUCCESS")
            .counter()
            .count());
    Timer timer =
        registry
            .get(MyBatisMetricsInterceptor.QUERY_DURATION_METRIC)
            .tags("statement", TAG_QUERY, "command", "SELECT", "outcome", "SUCCESS")
            .timer();
    assertEquals(2, timer.count());
  }

  @Test
  public void should_count_insert_statements() {
    articleRepository.save(new Article("title", "desc", "body", Arrays.asList("java"), "123"));

    assertNotNull(
        registry
            .get(MyBatisMetricsInterceptor.QUERY_COUNT_METRIC)
            .tags(
                "statement",
                "io.spring.infrastructure.mybatis.mapper.ArticleMapper.insert",
                "command",
                "INSERT")
            .counter());
  }
}
