package io.spring.application.article;

import io.spring.application.ArticleQueryService;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
  ArticleCommandService.class,
  ArticleQueryService.class,
  MyBatisArticleRepository.class,
  MyBatisUserRepository.class,
})
public class ArticleCommandServiceTest extends DbTestBase {

  @Autowired private ArticleCommandService articleCommandService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private UserRepository userRepository;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("test@test.com", "testuser", "123", "", "");
    userRepository.save(user);
  }

  @Test
  public void should_create_article_successfully() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Test Article")
            .description("Test Description")
            .body("Test Body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, user);

    Assertions.assertNotNull(article.getId());
    Assertions.assertEquals("Test Article", article.getTitle());
    Assertions.assertEquals("Test Description", article.getDescription());
    Assertions.assertEquals("Test Body", article.getBody());
    Assertions.assertEquals(user.getId(), article.getUserId());
    Assertions.assertEquals(2, article.getTags().size());

    Optional<Article> saved = articleRepository.findById(article.getId());
    Assertions.assertTrue(saved.isPresent());
    Assertions.assertEquals(article.getId(), saved.get().getId());
  }

  @Test
  public void should_create_article_without_tags() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("No Tag Article")
            .description("Desc")
            .body("Body")
            .tagList(Arrays.asList())
            .build();

    Article article = articleCommandService.createArticle(param, user);

    Assertions.assertNotNull(article.getId());
    Assertions.assertEquals("No Tag Article", article.getTitle());
    Assertions.assertTrue(article.getTags().isEmpty());

    Optional<Article> saved = articleRepository.findById(article.getId());
    Assertions.assertTrue(saved.isPresent());
  }

  @Test
  public void should_update_article_title() {
    Article article =
        new Article(
            "Original Title",
            "Original Desc",
            "Original Body",
            Arrays.asList("java"),
            user.getId());
    articleRepository.save(article);

    UpdateArticleParam updateParam = new UpdateArticleParam("Updated Title", "", "");
    Article updated = articleCommandService.updateArticle(article, updateParam);

    Assertions.assertEquals("Updated Title", updated.getTitle());

    Optional<Article> saved = articleRepository.findById(article.getId());
    Assertions.assertTrue(saved.isPresent());
    Assertions.assertEquals("Updated Title", saved.get().getTitle());
  }

  @Test
  public void should_update_article_all_fields() {
    Article article =
        new Article(
            "Original Title",
            "Original Desc",
            "Original Body",
            Arrays.asList("java"),
            user.getId());
    articleRepository.save(article);

    UpdateArticleParam updateParam = new UpdateArticleParam("New Title", "New Body", "New Desc");
    Article updated = articleCommandService.updateArticle(article, updateParam);

    Assertions.assertEquals("New Title", updated.getTitle());
    Assertions.assertEquals("New Body", updated.getBody());
    Assertions.assertEquals("New Desc", updated.getDescription());

    Optional<Article> saved = articleRepository.findById(article.getId());
    Assertions.assertTrue(saved.isPresent());
    Assertions.assertEquals("New Title", saved.get().getTitle());
  }
}
