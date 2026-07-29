package io.spring.application.article;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({ArticleCommandService.class, MyBatisArticleRepository.class, MyBatisUserRepository.class})
public class ArticleCommandServiceTest extends DbTestBase {
  @Autowired private ArticleCommandService articleCommandService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private UserRepository userRepository;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("aisensiy@gmail.com", "aisensiy", "123", "bio", "default");
    userRepository.save(user);
  }

  @Test
  public void should_create_article_with_slug_and_tags() {
    NewArticleParam newArticleParam =
        NewArticleParam.builder()
            .title("How to train your dragon")
            .description("Ever wonder how?")
            .body("You have to believe")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(newArticleParam, user);

    Assertions.assertEquals("how-to-train-your-dragon", article.getSlug());
    Assertions.assertEquals(user.getId(), article.getUserId());
    Assertions.assertEquals("Ever wonder how?", article.getDescription());
    Assertions.assertEquals("You have to believe", article.getBody());

    Optional<Article> saved = articleRepository.findBySlug("how-to-train-your-dragon");
    Assertions.assertTrue(saved.isPresent());
    Assertions.assertEquals(article.getId(), saved.get().getId());
    Assertions.assertEquals(Arrays.asList("java", "spring"), sortedTagNames(saved.get().getTags()));
  }

  @Test
  public void should_create_article_with_deduplicated_tags() {
    NewArticleParam newArticleParam =
        NewArticleParam.builder()
            .title("tags")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(newArticleParam, user);

    Assertions.assertEquals(Arrays.asList("java", "spring"), sortedTagNames(article.getTags()));
  }

  @Test
  public void should_create_article_without_tags() {
    NewArticleParam newArticleParam =
        NewArticleParam.builder()
            .title("no tags here")
            .description("desc")
            .body("body")
            .tagList(Collections.emptyList())
            .build();

    Article article = articleCommandService.createArticle(newArticleParam, user);

    Assertions.assertTrue(article.getTags().isEmpty());
    Assertions.assertTrue(articleRepository.findBySlug("no-tags-here").isPresent());
  }

  @Test
  public void should_update_article_and_regenerate_slug() {
    Article article = createArticle("old title");

    Article updated =
        articleCommandService.updateArticle(
            article, new UpdateArticleParam("new title", "new body", "new desc"));

    Assertions.assertEquals(article.getId(), updated.getId());
    Assertions.assertEquals("new-title", updated.getSlug());

    Optional<Article> saved = articleRepository.findBySlug("new-title");
    Assertions.assertTrue(saved.isPresent());
    Assertions.assertEquals("new title", saved.get().getTitle());
    Assertions.assertEquals("new desc", saved.get().getDescription());
    Assertions.assertEquals("new body", saved.get().getBody());
    Assertions.assertFalse(articleRepository.findBySlug("old-title").isPresent());
  }

  @Test
  public void should_keep_slug_and_fields_when_update_param_is_empty() {
    Article article = createArticle("keep me");

    Article updated =
        articleCommandService.updateArticle(article, new UpdateArticleParam("", "", ""));

    Assertions.assertEquals("keep-me", updated.getSlug());
    Assertions.assertEquals("keep me", updated.getTitle());
    Assertions.assertEquals("desc", updated.getDescription());
    Assertions.assertEquals("body", updated.getBody());

    Optional<Article> saved = articleRepository.findBySlug("keep-me");
    Assertions.assertTrue(saved.isPresent());
    Assertions.assertEquals("keep me", saved.get().getTitle());
  }

  @Test
  public void should_keep_tags_when_article_updated() {
    Article article = createArticle("tagged article");

    articleCommandService.updateArticle(article, new UpdateArticleParam("renamed article", "", ""));

    Optional<Article> saved = articleRepository.findBySlug("renamed-article");
    Assertions.assertTrue(saved.isPresent());
    Assertions.assertEquals(Arrays.asList("java", "spring"), sortedTagNames(saved.get().getTags()));
  }

  private Article createArticle(String title) {
    return articleCommandService.createArticle(
        NewArticleParam.builder()
            .title(title)
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "spring"))
            .build(),
        user);
  }

  private List<String> sortedTagNames(List<Tag> tags) {
    return tags.stream().map(Tag::getName).sorted().collect(Collectors.toList());
  }
}
