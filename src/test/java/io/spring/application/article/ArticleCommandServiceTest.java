package io.spring.application.article;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
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

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class ArticleCommandServiceTest {

  @Autowired private ArticleCommandService commandService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private UserRepository userRepository;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("test@example.com", "testuser", "password123", "bio", "avatar");
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
    Assertions.assertEquals(3, article.getTags().size());
    Assertions.assertTrue(article.getTags().contains(new Tag("java")));
    Assertions.assertTrue(article.getTags().contains(new Tag("spring")));
    Assertions.assertTrue(article.getTags().contains(new Tag("testing")));

    Optional<Article> savedArticle = articleRepository.findById(article.getId());
    Assertions.assertTrue(savedArticle.isPresent());
    Assertions.assertEquals(article.getId(), savedArticle.get().getId());
    Assertions.assertEquals(3, savedArticle.get().getTags().size());
  }

  @Test
  void should_create_article_without_tags() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Article Without Tags")
            .description("Description")
            .body("Body content")
            .tagList(Arrays.asList())
            .build();

    Article article = commandService.createArticle(param, user);

    Assertions.assertNotNull(article);
    Assertions.assertNotNull(article.getId());
    Assertions.assertEquals("Article Without Tags", article.getTitle());
    Assertions.assertTrue(article.getTags().isEmpty());

    Optional<Article> savedArticle = articleRepository.findById(article.getId());
    Assertions.assertTrue(savedArticle.isPresent());
  }

  @Test
  void should_create_article_with_empty_tag_list() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Article With Empty Tags")
            .description("Description")
            .body("Body content")
            .tagList(Arrays.asList())
            .build();

    Article article = commandService.createArticle(param, user);

    Assertions.assertNotNull(article);
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

    commandService.createArticle(firstParam, user);

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
  void should_reject_article_with_blank_title() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("")
            .description("Description")
            .body("Body content")
            .tagList(Arrays.asList("tag"))
            .build();

    Assertions.assertThrows(
        ConstraintViolationException.class, () -> commandService.createArticle(param, user));
  }

  @Test
  void should_reject_article_with_blank_description() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Valid Title")
            .description("")
            .body("Body content")
            .tagList(Arrays.asList("tag"))
            .build();

    Assertions.assertThrows(
        ConstraintViolationException.class, () -> commandService.createArticle(param, user));
  }

  @Test
  void should_reject_article_with_blank_body() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Valid Title")
            .description("Valid description")
            .body("")
            .tagList(Arrays.asList("tag"))
            .build();

    Assertions.assertThrows(
        ConstraintViolationException.class, () -> commandService.createArticle(param, user));
  }

  @Test
  void should_update_article_title() {
    NewArticleParam createParam =
        NewArticleParam.builder()
            .title("Original Title")
            .description("Original description")
            .body("Original body")
            .tagList(Arrays.asList("tag"))
            .build();

    Article article = commandService.createArticle(createParam, user);
    String originalSlug = article.getSlug();

    UpdateArticleParam updateParam = new UpdateArticleParam("Updated Title", "", "");

    Article updatedArticle = commandService.updateArticle(article, updateParam);

    Assertions.assertEquals("Updated Title", updatedArticle.getTitle());
    Assertions.assertNotEquals(originalSlug, updatedArticle.getSlug());
    Assertions.assertEquals("original-title", originalSlug);
    Assertions.assertEquals("updated-title", updatedArticle.getSlug());
  }

  @Test
  void should_update_article_description_only() {
    NewArticleParam createParam =
        NewArticleParam.builder()
            .title("Test Title")
            .description("Original description")
            .body("Original body")
            .tagList(Arrays.asList("tag"))
            .build();

    Article article = commandService.createArticle(createParam, user);
    String originalTitle = article.getTitle();
    String originalBody = article.getBody();

    UpdateArticleParam updateParam = new UpdateArticleParam("", "", "Updated description");

    Article updatedArticle = commandService.updateArticle(article, updateParam);

    Assertions.assertEquals("Updated description", updatedArticle.getDescription());
    Assertions.assertEquals(originalTitle, updatedArticle.getTitle());
    Assertions.assertEquals(originalBody, updatedArticle.getBody());
  }

  @Test
  void should_update_article_body_only() {
    NewArticleParam createParam =
        NewArticleParam.builder()
            .title("Test Title")
            .description("Test description")
            .body("Original body")
            .tagList(Arrays.asList("tag"))
            .build();

    Article article = commandService.createArticle(createParam, user);
    String originalTitle = article.getTitle();
    String originalDescription = article.getDescription();

    UpdateArticleParam updateParam = new UpdateArticleParam("", "Updated body content", "");

    Article updatedArticle = commandService.updateArticle(article, updateParam);

    Assertions.assertEquals("Updated body content", updatedArticle.getBody());
    Assertions.assertEquals(originalTitle, updatedArticle.getTitle());
    Assertions.assertEquals(originalDescription, updatedArticle.getDescription());
  }

  @Test
  void should_update_multiple_fields() {
    NewArticleParam createParam =
        NewArticleParam.builder()
            .title("Test Title")
            .description("Test description")
            .body("Test body")
            .tagList(Arrays.asList("tag"))
            .build();

    Article article = commandService.createArticle(createParam, user);

    UpdateArticleParam updateParam =
        new UpdateArticleParam("New Title", "New body", "New description");

    Article updatedArticle = commandService.updateArticle(article, updateParam);

    Assertions.assertEquals("New Title", updatedArticle.getTitle());
    Assertions.assertEquals("New description", updatedArticle.getDescription());
    Assertions.assertEquals("New body", updatedArticle.getBody());
    Assertions.assertEquals("new-title", updatedArticle.getSlug());
  }

  @Test
  void should_persist_updated_article() {
    NewArticleParam createParam =
        NewArticleParam.builder()
            .title("Persist Test")
            .description("Test description")
            .body("Test body")
            .tagList(Arrays.asList("tag"))
            .build();

    Article article = commandService.createArticle(createParam, user);

    UpdateArticleParam updateParam = new UpdateArticleParam("Updated Persist Test", "", "");

    commandService.updateArticle(article, updateParam);

    Optional<Article> fetchedArticle = articleRepository.findBySlug("updated-persist-test");
    Assertions.assertTrue(fetchedArticle.isPresent());
    Assertions.assertEquals("Updated Persist Test", fetchedArticle.get().getTitle());
  }

  @Test
  void should_generate_correct_slug_from_title() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("This Is A Test Title")
            .description("Description")
            .body("Body")
            .tagList(Arrays.asList("tag"))
            .build();

    Article article = commandService.createArticle(param, user);

    Assertions.assertEquals("this-is-a-test-title", article.getSlug());
  }

  @Test
  void should_handle_duplicate_tags_in_list() {
    List<String> tagListWithDuplicates = Arrays.asList("java", "spring", "java", "spring", "test");
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
    Assertions.assertTrue(article.getTags().contains(new Tag("test")));
  }
}
