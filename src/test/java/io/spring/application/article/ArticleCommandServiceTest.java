package io.spring.application.article;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ArticleCommandServiceTest {

  @Autowired private ArticleCommandService commandService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private UserRepository userRepository;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User("test@example.com", "testuser", "password123", "bio", "image");
    userRepository.save(user);
  }

  @Test
  void should_create_article_with_tags() {
    List<String> tagList = Arrays.asList("java", "spring", "testing");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Test Article Title")
            .description("Test description")
            .body("Test body content")
            .tagList(tagList)
            .build();

    Article article = commandService.createArticle(param, user);

    Assertions.assertNotNull(article);
    Assertions.assertNotNull(article.getId());
    Assertions.assertEquals("Test Article Title", article.getTitle());
    Assertions.assertEquals("Test description", article.getDescription());
    Assertions.assertEquals("Test body content", article.getBody());
    Assertions.assertEquals(user.getId(), article.getUserId());
    Assertions.assertEquals("test-article-title", article.getSlug());
    Assertions.assertEquals(3, article.getTags().size());
    Assertions.assertTrue(article.getTags().contains(new Tag("java")));
    Assertions.assertTrue(article.getTags().contains(new Tag("spring")));
    Assertions.assertTrue(article.getTags().contains(new Tag("testing")));

    Optional<Article> fetched = articleRepository.findById(article.getId());
    Assertions.assertTrue(fetched.isPresent());
    Assertions.assertEquals(article.getId(), fetched.get().getId());
    Assertions.assertEquals(3, fetched.get().getTags().size());
  }

  @Test
  void should_create_article_without_tags() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Article Without Tags")
            .description("Description")
            .body("Body content")
            .tagList(Collections.emptyList())
            .build();

    Article article = commandService.createArticle(param, user);

    Assertions.assertNotNull(article);
    Assertions.assertEquals("Article Without Tags", article.getTitle());
    Assertions.assertNotNull(article.getTags());
    Assertions.assertTrue(article.getTags().isEmpty());
  }

  @Test
  void should_validate_article_uniqueness() {
    NewArticleParam firstParam =
        NewArticleParam.builder()
            .title("Unique Title")
            .description("First description")
            .body("First body")
            .tagList(Arrays.asList("tag1"))
            .build();

    Article firstArticle = commandService.createArticle(firstParam, user);
    Assertions.assertNotNull(firstArticle);

    NewArticleParam duplicateParam =
        NewArticleParam.builder()
            .title("Unique Title")
            .description("Second description")
            .body("Second body")
            .tagList(Arrays.asList("tag2"))
            .build();

    Assertions.assertThrows(
        ConstraintViolationException.class,
        () -> commandService.createArticle(duplicateParam, user));
  }

  @Test
  void should_update_article_title() {
    NewArticleParam createParam =
        NewArticleParam.builder()
            .title("Original Title")
            .description("Original description")
            .body("Original body")
            .tagList(Arrays.asList("tag1"))
            .build();

    Article article = commandService.createArticle(createParam, user);
    String originalSlug = article.getSlug();

    UpdateArticleParam updateParam = new UpdateArticleParam("Updated Title", "", "");

    Article updatedArticle = commandService.updateArticle(article, updateParam);

    Assertions.assertEquals("Updated Title", updatedArticle.getTitle());
    Assertions.assertEquals("updated-title", updatedArticle.getSlug());
    Assertions.assertNotEquals(originalSlug, updatedArticle.getSlug());
    Assertions.assertEquals("Original description", updatedArticle.getDescription());
    Assertions.assertEquals("Original body", updatedArticle.getBody());
  }

  @Test
  void should_update_article_description() {
    NewArticleParam createParam =
        NewArticleParam.builder()
            .title("Title For Description Update")
            .description("Original description")
            .body("Original body")
            .tagList(Arrays.asList("tag1"))
            .build();

    Article article = commandService.createArticle(createParam, user);

    UpdateArticleParam updateParam = new UpdateArticleParam("", "", "Updated description");

    Article updatedArticle = commandService.updateArticle(article, updateParam);

    Assertions.assertEquals("Title For Description Update", updatedArticle.getTitle());
    Assertions.assertEquals("Updated description", updatedArticle.getDescription());
    Assertions.assertEquals("Original body", updatedArticle.getBody());
  }

  @Test
  void should_update_article_body() {
    NewArticleParam createParam =
        NewArticleParam.builder()
            .title("Title For Body Update")
            .description("Original description")
            .body("Original body")
            .tagList(Arrays.asList("tag1"))
            .build();

    Article article = commandService.createArticle(createParam, user);

    UpdateArticleParam updateParam = new UpdateArticleParam("", "Updated body", "");

    Article updatedArticle = commandService.updateArticle(article, updateParam);

    Assertions.assertEquals("Title For Body Update", updatedArticle.getTitle());
    Assertions.assertEquals("Original description", updatedArticle.getDescription());
    Assertions.assertEquals("Updated body", updatedArticle.getBody());
  }

  @Test
  void should_update_all_article_fields() {
    NewArticleParam createParam =
        NewArticleParam.builder()
            .title("Original Title")
            .description("Original description")
            .body("Original body")
            .tagList(Arrays.asList("tag1"))
            .build();

    Article article = commandService.createArticle(createParam, user);

    UpdateArticleParam updateParam =
        new UpdateArticleParam("New Title", "New body", "New description");

    Article updatedArticle = commandService.updateArticle(article, updateParam);

    Assertions.assertEquals("New Title", updatedArticle.getTitle());
    Assertions.assertEquals("new-title", updatedArticle.getSlug());
    Assertions.assertEquals("New description", updatedArticle.getDescription());
    Assertions.assertEquals("New body", updatedArticle.getBody());
  }

  @Test
  void should_persist_updated_article() {
    NewArticleParam createParam =
        NewArticleParam.builder()
            .title("Persist Update Test")
            .description("Original description")
            .body("Original body")
            .tagList(Arrays.asList("tag1"))
            .build();

    Article article = commandService.createArticle(createParam, user);
    String articleId = article.getId();

    UpdateArticleParam updateParam =
        new UpdateArticleParam("Persisted Updated Title", "Updated body", "Updated description");

    commandService.updateArticle(article, updateParam);

    Optional<Article> fetched = articleRepository.findById(articleId);
    Assertions.assertTrue(fetched.isPresent());
    Assertions.assertEquals("Persisted Updated Title", fetched.get().getTitle());
    Assertions.assertEquals("Updated description", fetched.get().getDescription());
    Assertions.assertEquals("Updated body", fetched.get().getBody());
  }

  @Test
  void should_create_article_with_duplicate_tags_deduplicated() {
    List<String> tagListWithDuplicates = Arrays.asList("java", "spring", "java", "testing", "spring");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Article With Duplicate Tags")
            .description("Description")
            .body("Body")
            .tagList(tagListWithDuplicates)
            .build();

    Article article = commandService.createArticle(param, user);

    Assertions.assertEquals(3, article.getTags().size());
    Assertions.assertTrue(article.getTags().contains(new Tag("java")));
    Assertions.assertTrue(article.getTags().contains(new Tag("spring")));
    Assertions.assertTrue(article.getTags().contains(new Tag("testing")));
  }

  @Test
  void should_generate_slug_from_title() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("This Is A Test Title With Spaces")
            .description("Description")
            .body("Body")
            .tagList(Arrays.asList("tag1"))
            .build();

    Article article = commandService.createArticle(param, user);

    Assertions.assertEquals("this-is-a-test-title-with-spaces", article.getSlug());
  }

  @Test
  void should_set_timestamps_on_create() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Timestamp Test Article")
            .description("Description")
            .body("Body")
            .tagList(Arrays.asList("tag1"))
            .build();

    Article article = commandService.createArticle(param, user);

    Assertions.assertNotNull(article.getCreatedAt());
    Assertions.assertNotNull(article.getUpdatedAt());
    Assertions.assertEquals(article.getCreatedAt(), article.getUpdatedAt());
  }
}
