package io.spring.application.article;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
  ArticleCommandService.class,
  MyBatisUserRepository.class,
  MyBatisArticleRepository.class
})
public class ArticleCommandServiceTest extends DbTestBase {

  @Autowired private ArticleCommandService articleCommandService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private UserRepository userRepository;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("test@example.com", "testuser", "password123", "Test Bio", "http://image.url");
    userRepository.save(user);
  }

  @Test
  public void should_create_article_with_valid_params() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Test Article")
            .description("Test Description")
            .body("Test Body Content")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, user);

    Assertions.assertNotNull(article);
    Assertions.assertEquals("Test Article", article.getTitle());
    Assertions.assertEquals("Test Description", article.getDescription());
    Assertions.assertEquals("Test Body Content", article.getBody());
    Assertions.assertEquals(user.getId(), article.getUserId());
    Assertions.assertNotNull(article.getId());
    Assertions.assertNotNull(article.getSlug());

    Optional<Article> savedArticle = articleRepository.findById(article.getId());
    Assertions.assertTrue(savedArticle.isPresent());
    Assertions.assertEquals("Test Article", savedArticle.get().getTitle());
  }

  @Test
  public void should_create_article_with_empty_tag_list() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Article Without Tags")
            .description("Description")
            .body("Body content")
            .tagList(Collections.emptyList())
            .build();

    Article article = articleCommandService.createArticle(param, user);

    Assertions.assertNotNull(article);
    Assertions.assertEquals("Article Without Tags", article.getTitle());
    Assertions.assertNotNull(article.getId());
  }

  @Test
  public void should_create_article_with_multiple_tags() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Multi Tag Article")
            .description("Description")
            .body("Body content")
            .tagList(Arrays.asList("java", "spring", "boot", "test"))
            .build();

    Article article = articleCommandService.createArticle(param, user);

    Assertions.assertNotNull(article);
    Assertions.assertEquals("Multi Tag Article", article.getTitle());
  }

  @Test
  public void should_update_article_title() {
    Article article = new Article("Original Title", "Description", "Body", Arrays.asList("tag"), user.getId());
    articleRepository.save(article);

    UpdateArticleParam updateParam = new UpdateArticleParam("Updated Title", "Body", "Description");

    Article updatedArticle = articleCommandService.updateArticle(article, updateParam);

    Assertions.assertNotNull(updatedArticle);
    Assertions.assertEquals("Updated Title", updatedArticle.getTitle());
    Assertions.assertEquals("Body", updatedArticle.getBody());
    Assertions.assertEquals("Description", updatedArticle.getDescription());

    Optional<Article> savedArticle = articleRepository.findById(article.getId());
    Assertions.assertTrue(savedArticle.isPresent());
    Assertions.assertEquals("Updated Title", savedArticle.get().getTitle());
  }

  @Test
  public void should_update_article_description() {
    Article article = new Article("Title", "Original Description", "Body", Arrays.asList("tag"), user.getId());
    articleRepository.save(article);

    UpdateArticleParam updateParam = new UpdateArticleParam("Title", "Body", "Updated Description");

    Article updatedArticle = articleCommandService.updateArticle(article, updateParam);

    Assertions.assertNotNull(updatedArticle);
    Assertions.assertEquals("Updated Description", updatedArticle.getDescription());
  }

  @Test
  public void should_update_article_body() {
    Article article = new Article("Title", "Description", "Original Body", Arrays.asList("tag"), user.getId());
    articleRepository.save(article);

    UpdateArticleParam updateParam = new UpdateArticleParam("Title", "Updated Body Content", "Description");

    Article updatedArticle = articleCommandService.updateArticle(article, updateParam);

    Assertions.assertNotNull(updatedArticle);
    Assertions.assertEquals("Updated Body Content", updatedArticle.getBody());
  }

  @Test
  public void should_update_all_article_fields() {
    Article article = new Article("Original Title", "Original Description", "Original Body", Arrays.asList("tag"), user.getId());
    articleRepository.save(article);

    UpdateArticleParam updateParam = new UpdateArticleParam("New Title", "New Body", "New Description");

    Article updatedArticle = articleCommandService.updateArticle(article, updateParam);

    Assertions.assertNotNull(updatedArticle);
    Assertions.assertEquals("New Title", updatedArticle.getTitle());
    Assertions.assertEquals("New Body", updatedArticle.getBody());
    Assertions.assertEquals("New Description", updatedArticle.getDescription());
  }

  @Test
  public void should_preserve_article_id_after_update() {
    Article article = new Article("Title", "Description", "Body", Arrays.asList("tag"), user.getId());
    articleRepository.save(article);
    String originalId = article.getId();

    UpdateArticleParam updateParam = new UpdateArticleParam("Updated Title", "Updated Body", "Updated Description");

    Article updatedArticle = articleCommandService.updateArticle(article, updateParam);

    Assertions.assertEquals(originalId, updatedArticle.getId());
  }

  @Test
  public void should_create_articles_by_different_users() {
    User anotherUser = new User("another@example.com", "anotheruser", "password", "Bio", "http://image.url");
    userRepository.save(anotherUser);

    NewArticleParam param1 =
        NewArticleParam.builder()
            .title("Article by User 1")
            .description("Description 1")
            .body("Body 1")
            .tagList(Arrays.asList("tag1"))
            .build();

    NewArticleParam param2 =
        NewArticleParam.builder()
            .title("Article by User 2")
            .description("Description 2")
            .body("Body 2")
            .tagList(Arrays.asList("tag2"))
            .build();

    Article article1 = articleCommandService.createArticle(param1, user);
    Article article2 = articleCommandService.createArticle(param2, anotherUser);

    Assertions.assertEquals(user.getId(), article1.getUserId());
    Assertions.assertEquals(anotherUser.getId(), article2.getUserId());
    Assertions.assertNotEquals(article1.getId(), article2.getId());
  }
}
