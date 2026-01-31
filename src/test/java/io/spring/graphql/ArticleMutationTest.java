package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.ArticleQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
public class ArticleMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleCommandService articleCommandService;

  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleQueryService articleQueryService;

  private User user;
  private Article article;
  private ArticleData articleData;

  @BeforeEach
  public void setUp() {
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    DateTime now = new DateTime();
    article = new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), user.getId(), now);

    ProfileData profileData =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
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
            Arrays.asList("java"),
            profileData);
  }

  @Test
  public void should_create_article() {
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), any()))
        .thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { createArticle(input: { title: \"Test Title\", description: \"Test Description\", body: \"Test Body\", tagList: [\"java\"] }) { article { slug title } } }";

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createArticle.article.slug");

    assert slug.equals(article.getSlug());
    verify(articleCommandService).createArticle(any(), eq(user));
  }

  @Test
  public void should_update_article() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), any()))
        .thenReturn(Optional.of(articleData));

    String mutation =
        String.format(
            "mutation { updateArticle(slug: \"%s\", changes: { title: \"Updated Title\" }) { article { slug } } }",
            article.getSlug());

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateArticle.article.slug");

    assert slug != null;
    verify(articleCommandService).updateArticle(eq(article), any());
  }

  @Test
  public void should_favorite_article() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleQueryService.findById(eq(article.getId()), any()))
        .thenReturn(Optional.of(articleData));

    String mutation =
        String.format(
            "mutation { favoriteArticle(slug: \"%s\") { article { slug favorited } } }",
            article.getSlug());

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.favoriteArticle.article.slug");

    assert slug.equals(article.getSlug());
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  public void should_unfavorite_article() {
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));
    when(articleQueryService.findById(eq(article.getId()), any()))
        .thenReturn(Optional.of(articleData));

    String mutation =
        String.format(
            "mutation { unfavoriteArticle(slug: \"%s\") { article { slug } } }", article.getSlug());

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfavoriteArticle.article.slug");

    assert slug.equals(article.getSlug());
    verify(articleFavoriteRepository).remove(eq(favorite));
  }

  @Test
  public void should_delete_article() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    String mutation =
        String.format("mutation { deleteArticle(slug: \"%s\") { success } }", article.getSlug());

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteArticle.success");

    assert success;
    verify(articleRepository).remove(eq(article));
  }

  @Test
  public void should_fail_create_article_without_auth() {
    SecurityContextHolder.clearContext();

    String mutation =
        "mutation { createArticle(input: { title: \"Test\", description: \"Test\", body: \"Test\" }) { article { slug } } }";

    try {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createArticle.article.slug");
      assert false : "Should have thrown exception";
    } catch (Exception e) {
      assert true;
    }
  }
}
