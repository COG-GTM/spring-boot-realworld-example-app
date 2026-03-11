package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.ArticleQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class ArticleMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleCommandService articleCommandService;

  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private ProfileQueryService profileQueryService;

  @MockBean private UserRepository userRepository;

  private User user;
  private Article article;
  private ArticleData articleData;

  @BeforeEach
  public void setUp() {
    user = new User(
        "test@example.com",
        "testuser",
        "password",
        "bio",
        "image");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null));

    article =
        new Article(
            "Test Title",
            "Test Description",
            "Test Body",
            Arrays.asList("tag1", "tag2"),
            user.getId());

    DateTime now = new DateTime();
    articleData =
        new ArticleData(
            article.getId(),
            article.getSlug(),
            article.getTitle(),
            article.getDescription(),
            article.getBody(),
            false,
            0,
            now,
            now,
            Arrays.asList("tag1", "tag2"),
            new ProfileData(
                user.getId(),
                user.getUsername(),
                user.getBio(),
                user.getImage(),
                false));
  }

  @Test
  public void should_create_article_success() {
    when(articleCommandService.createArticle(any(), eq(user)))
        .thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), eq(user)))
        .thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { createArticle(input: {"
            + "title: \"Test Title\", "
            + "description: \"Test Description\", "
            + "body: \"Test Body\", "
            + "tagList: [\"tag1\", \"tag2\"]}) "
            + "{ article { slug title description body tagList } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(
            mutation, "data.createArticle");
    assertNotNull(result);
  }

  @Test
  public void should_update_article_success() {
    when(articleRepository.findBySlug(eq(article.getSlug())))
        .thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any()))
        .thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), eq(user)))
        .thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { updateArticle(slug: \""
            + article.getSlug()
            + "\", changes: {"
            + "title: \"Updated Title\", "
            + "description: \"Updated Description\", "
            + "body: \"Updated Body\"}) "
            + "{ article { slug title description body } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(
            mutation, "data.updateArticle");
    assertNotNull(result);
  }

  @Test
  public void should_favorite_article_success() {
    when(articleRepository.findBySlug(eq(article.getSlug())))
        .thenReturn(Optional.of(article));
    when(articleQueryService.findById(eq(article.getId()), eq(user)))
        .thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { favoriteArticle(slug: \""
            + article.getSlug()
            + "\") { article { slug favorited } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(
            mutation, "data.favoriteArticle");
    assertNotNull(result);
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  public void should_unfavorite_article_success() {
    ArticleFavorite favorite =
        new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug())))
        .thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(
            eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));
    when(articleQueryService.findById(eq(article.getId()), eq(user)))
        .thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { unfavoriteArticle(slug: \""
            + article.getSlug()
            + "\") { article { slug favorited } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(
            mutation, "data.unfavoriteArticle");
    assertNotNull(result);
  }

  @Test
  public void should_delete_article_success() {
    when(articleRepository.findBySlug(eq(article.getSlug())))
        .thenReturn(Optional.of(article));

    String mutation =
        "mutation { deleteArticle(slug: \""
            + article.getSlug()
            + "\") { success } }";

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(
            mutation, "data.deleteArticle.success");
    assertNotNull(success);
    assertTrue(success);
    verify(articleRepository).remove(eq(article));
  }

  @Test
  public void should_fail_update_article_without_authorization() {
    User anotherUser = new User(
        "another@example.com",
        "anotheruser",
        "password",
        "",
        "");
    Article anotherArticle =
        new Article(
            "Another Title",
            "Another Description",
            "Another Body",
            Arrays.asList("tag1"),
            anotherUser.getId());

    when(articleRepository.findBySlug(eq(anotherArticle.getSlug())))
        .thenReturn(Optional.of(anotherArticle));

    String mutation =
        "mutation { updateArticle(slug: \""
            + anotherArticle.getSlug()
            + "\", changes: {title: \"Updated Title\"}) "
            + "{ article { slug } } }";

    assertThrows(
        Exception.class,
        () -> {
          dgsQueryExecutor.executeAndExtractJsonPath(
              mutation, "data.updateArticle");
        });
  }

  @Test
  public void should_fail_delete_article_without_authorization() {
    User anotherUser = new User(
        "another@example.com",
        "anotheruser",
        "password",
        "",
        "");
    Article anotherArticle =
        new Article(
            "Another Title",
            "Another Description",
            "Another Body",
            Arrays.asList("tag1"),
            anotherUser.getId());

    when(articleRepository.findBySlug(eq(anotherArticle.getSlug())))
        .thenReturn(Optional.of(anotherArticle));

    String mutation =
        "mutation { deleteArticle(slug: \""
            + anotherArticle.getSlug()
            + "\") { success } }";

    assertThrows(
        Exception.class,
        () -> {
          dgsQueryExecutor.executeAndExtractJsonPath(
              mutation, "data.deleteArticle");
        });
  }

  @Test
  public void should_fail_create_article_without_authentication() {
    SecurityContextHolder.clearContext();

    String mutation =
        "mutation { createArticle(input: {"
            + "title: \"Test Title\", "
            + "description: \"Test Description\", "
            + "body: \"Test Body\"}) "
            + "{ article { slug } } }";

    assertThrows(
        Exception.class,
        () -> {
          dgsQueryExecutor.executeAndExtractJsonPath(
              mutation, "data.createArticle");
        });
  }
}
