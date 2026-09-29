package io.spring.application.article;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArticleCommandServiceTest {
  private ArticleRepository articleRepository;
  private ArticleCommandService articleCommandService;
  private Article article;

  @BeforeEach
  public void setUp() {
    articleRepository = mock(ArticleRepository.class);
    articleCommandService = new ArticleCommandService(articleRepository);
    article = new Article("title", "desc", "body", Arrays.asList("java"), "user-id");
  }

  @Test
  public void should_soft_delete_and_persist_article() {
    articleCommandService.deleteArticle(article);

    Assertions.assertTrue(article.isDeleted());
    verify(articleRepository).save(article);
  }

  @Test
  public void should_restore_and_persist_article() {
    article.softDelete();

    Article restored = articleCommandService.restoreArticle(article);

    Assertions.assertSame(article, restored);
    Assertions.assertFalse(restored.isDeleted());
    verify(articleRepository).save(article);
  }
}
