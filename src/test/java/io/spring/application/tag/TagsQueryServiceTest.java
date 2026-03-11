package io.spring.application.tag;

import io.spring.application.TagsQueryService;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({TagsQueryService.class, MyBatisArticleRepository.class})
public class TagsQueryServiceTest extends DbTestBase {
  @Autowired private TagsQueryService tagsQueryService;

  @Autowired private ArticleRepository articleRepository;

  @Test
  public void should_get_all_tags() {
    articleRepository.save(new Article("test", "test", "test", Arrays.asList("java"), "123"));
    Assertions.assertTrue(tagsQueryService.allTags().contains("java"));
  }

  @Test
  public void should_return_empty_list_when_no_tags() {
    List<String> tags = tagsQueryService.allTags();
    Assertions.assertTrue(tags.isEmpty());
  }

  @Test
  public void should_get_multiple_tags() {
    articleRepository.save(
        new Article("test1", "test", "test", Arrays.asList("java", "spring"), "123"));
    articleRepository.save(
        new Article("test2", "test", "test", Arrays.asList("kotlin", "android"), "456"));

    List<String> tags = tagsQueryService.allTags();
    Assertions.assertTrue(tags.contains("java"));
    Assertions.assertTrue(tags.contains("spring"));
    Assertions.assertTrue(tags.contains("kotlin"));
    Assertions.assertTrue(tags.contains("android"));
  }

  @Test
  public void should_not_return_duplicate_tags() {
    articleRepository.save(new Article("test1", "test", "test", Arrays.asList("java"), "123"));
    articleRepository.save(new Article("test2", "test", "test", Arrays.asList("java"), "456"));

    List<String> tags = tagsQueryService.allTags();
    long javaCount = tags.stream().filter(tag -> tag.equals("java")).count();
    Assertions.assertEquals(1, javaCount);
  }

  @Test
  public void should_get_tags_from_article_with_multiple_tags() {
    articleRepository.save(
        new Article(
            "test", "test", "test", Arrays.asList("java", "spring", "boot", "rest"), "123"));

    List<String> tags = tagsQueryService.allTags();
    Assertions.assertEquals(4, tags.size());
    Assertions.assertTrue(tags.contains("java"));
    Assertions.assertTrue(tags.contains("spring"));
    Assertions.assertTrue(tags.contains("boot"));
    Assertions.assertTrue(tags.contains("rest"));
  }
}
