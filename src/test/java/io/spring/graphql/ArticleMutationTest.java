package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
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
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {DgsAutoConfiguration.class, ArticleMutation.class, ArticleDatafetcher.class})
@Import({ProfileDatafetcher.class, CommentDatafetcher.class})
@ActiveProfiles("test")
public class ArticleMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleCommandService articleCommandService;

  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private io.spring.core.user.UserRepository userRepository;

  @MockBean private io.spring.application.ProfileQueryService profileQueryService;

  @MockBean private io.spring.application.CommentQueryService commentQueryService;

  private User user;
  private Article article;
  private ArticleData articleData;
  private ProfileData profileData;

  @BeforeEach
  void setUp() {
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    DateTime now = DateTime.now();
    article = new Article("Test Article", "Test Description", "Test Body", Arrays.asList("tag1", "tag2"), user.getId());
    articleData = new ArticleData(
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
        profileData);

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, null));
  }

  @Test
  void shouldCreateArticle() {
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq("testuser"), any())).thenReturn(Optional.of(profileData));

    String mutation = "mutation { createArticle(input: { title: \"Test Article\", description: \"Test Description\", body: \"Test Body\", tagList: [\"tag1\", \"tag2\"] }) { article { slug title description body tagList } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createArticle.article");

    assertThat(result.get("slug")).isEqualTo(article.getSlug());
    assertThat(result.get("title")).isEqualTo("Test Article");
    assertThat(result.get("description")).isEqualTo("Test Description");
    assertThat(result.get("body")).isEqualTo("Test Body");
  }

  @Test
  void shouldUpdateArticle() {
    Article updatedArticle = new Article("Updated Title", "Updated Description", "Updated Body", Arrays.asList("tag1"), user.getId());
    ArticleData updatedArticleData = new ArticleData(
        updatedArticle.getId(),
        updatedArticle.getSlug(),
        updatedArticle.getTitle(),
        updatedArticle.getDescription(),
        updatedArticle.getBody(),
        false,
        0,
        DateTime.now(),
        DateTime.now(),
        Arrays.asList("tag1"),
        profileData);

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(updatedArticle);
    when(articleQueryService.findById(eq(updatedArticle.getId()), any())).thenReturn(Optional.of(updatedArticleData));
    when(profileQueryService.findByUsername(eq("testuser"), any())).thenReturn(Optional.of(profileData));

    String mutation = String.format(
        "mutation { updateArticle(slug: \"%s\", changes: { title: \"Updated Title\", description: \"Updated Description\", body: \"Updated Body\" }) { article { slug title description body } } }",
        article.getSlug());

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateArticle.article");

    assertThat(result.get("title")).isEqualTo("Updated Title");
    assertThat(result.get("description")).isEqualTo("Updated Description");
    assertThat(result.get("body")).isEqualTo("Updated Body");
  }

  @Test
  void shouldReturnErrorWhenUpdatingNonExistentArticle() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    String mutation = "mutation { updateArticle(slug: \"non-existent\", changes: { title: \"Updated\" }) { article { slug } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldReturnErrorWhenUpdatingArticleWithoutAuth() {
    SecurityContextHolder.clearContext();

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    String mutation = String.format(
        "mutation { updateArticle(slug: \"%s\", changes: { title: \"Updated\" }) { article { slug } } }",
        article.getSlug());

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldFavoriteArticle() {
    ArticleData favoritedArticleData = new ArticleData(
        article.getId(),
        article.getSlug(),
        article.getTitle(),
        article.getDescription(),
        article.getBody(),
        true,
        1,
        DateTime.now(),
        DateTime.now(),
        Arrays.asList("tag1", "tag2"),
        profileData);

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(favoritedArticleData));
    when(profileQueryService.findByUsername(eq("testuser"), any())).thenReturn(Optional.of(profileData));

    String mutation = String.format(
        "mutation { favoriteArticle(slug: \"%s\") { article { slug favorited favoritesCount } } }",
        article.getSlug());

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.favoriteArticle.article");

    assertThat(result.get("favorited")).isEqualTo(true);
    assertThat(result.get("favoritesCount")).isEqualTo(1);
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  void shouldUnfavoriteArticle() {
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId()))).thenReturn(Optional.of(favorite));
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq("testuser"), any())).thenReturn(Optional.of(profileData));

    String mutation = String.format(
        "mutation { unfavoriteArticle(slug: \"%s\") { article { slug favorited favoritesCount } } }",
        article.getSlug());

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfavoriteArticle.article");

    assertThat(result.get("favorited")).isEqualTo(false);
    verify(articleFavoriteRepository).remove(eq(favorite));
  }

  @Test
  void shouldDeleteArticle() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    String mutation = String.format(
        "mutation { deleteArticle(slug: \"%s\") { success } }",
        article.getSlug());

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteArticle");

    assertThat(result.get("success")).isEqualTo(true);
    verify(articleRepository).remove(eq(article));
  }

  @Test
  void shouldReturnErrorWhenDeletingNonExistentArticle() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    String mutation = "mutation { deleteArticle(slug: \"non-existent\") { success } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldReturnErrorWhenDeletingArticleWithoutAuth() {
    SecurityContextHolder.clearContext();

    String mutation = String.format(
        "mutation { deleteArticle(slug: \"%s\") { success } }",
        article.getSlug());

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldReturnErrorWhenFavoritingWithoutAuth() {
    SecurityContextHolder.clearContext();

    String mutation = String.format(
        "mutation { favoriteArticle(slug: \"%s\") { article { slug } } }",
        article.getSlug());

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldReturnErrorWhenCreatingArticleWithoutAuth() {
    SecurityContextHolder.clearContext();

    String mutation = "mutation { createArticle(input: { title: \"Test\", description: \"Test\", body: \"Test\" }) { article { slug } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }
}
