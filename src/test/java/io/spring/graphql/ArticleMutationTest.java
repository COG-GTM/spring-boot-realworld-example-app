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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(classes = {DgsAutoConfiguration.class, ArticleMutation.class, ArticleDatafetcher.class})
public class ArticleMutationTest {

  @Autowired
  private DgsQueryExecutor dgsQueryExecutor;

  @MockBean
  private ArticleCommandService articleCommandService;

  @MockBean
  private ArticleFavoriteRepository articleFavoriteRepository;

  @MockBean
  private ArticleRepository articleRepository;

  @MockBean
  private ArticleQueryService articleQueryService;

  @MockBean
  private io.spring.core.user.UserRepository userRepository;

  private User user;
  private Article article;
  private ArticleData articleData;

  @BeforeEach
  void setUp() {
    user = new User("user@example.com", "testuser", "password", "bio", "image");
    DateTime now = new DateTime();
    article = new Article("Test Article", "Test Description", "Test Body", Arrays.asList("java", "spring"), user.getId(), now);

    ProfileData profileData = new ProfileData(
        user.getId(),
        user.getUsername(),
        user.getBio(),
        user.getImage(),
        false);
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
        Arrays.asList("java", "spring"),
        profileData);

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, Arrays.asList()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldCreateArticle() {
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(articleData));

    String mutation = "mutation { createArticle(input: {title: \"Test Article\", description: \"Test Description\", body: \"Test Body\", tagList: [\"java\", \"spring\"]}) { article { slug title description body tagList } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createArticle.article");

    assertThat(result).isNotNull();
    assertThat(result.get("slug")).isEqualTo(article.getSlug());
    assertThat(result.get("title")).isEqualTo("Test Article");
  }

  @Test
  void shouldUpdateArticle() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    Article updatedArticle = new Article("Updated Title", "Updated Description", "Updated Body", Arrays.asList("java"), user.getId());
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(updatedArticle);

    DateTime now = new DateTime();
    ProfileData profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    ArticleData updatedArticleData = new ArticleData(
        updatedArticle.getId(),
        updatedArticle.getSlug(),
        updatedArticle.getTitle(),
        updatedArticle.getDescription(),
        updatedArticle.getBody(),
        false,
        0,
        now,
        now,
        Arrays.asList("java"),
        profileData);
    when(articleQueryService.findById(eq(updatedArticle.getId()), any())).thenReturn(Optional.of(updatedArticleData));

    String mutation = String.format(
        "mutation { updateArticle(slug: \"%s\", changes: {title: \"Updated Title\", description: \"Updated Description\", body: \"Updated Body\"}) { article { slug title } } }",
        article.getSlug());

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateArticle.article");

    assertThat(result).isNotNull();
    assertThat(result.get("title")).isEqualTo("Updated Title");
  }

  @Test
  void shouldFavoriteArticle() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(articleData));

    String mutation = String.format(
        "mutation { favoriteArticle(slug: \"%s\") { article { slug favorited } } }",
        article.getSlug());

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.favoriteArticle.article");

    assertThat(result).isNotNull();
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  void shouldUnfavoriteArticle() {
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId()))).thenReturn(Optional.of(favorite));
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(articleData));

    String mutation = String.format(
        "mutation { unfavoriteArticle(slug: \"%s\") { article { slug } } }",
        article.getSlug());

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfavoriteArticle.article");

    assertThat(result).isNotNull();
    verify(articleFavoriteRepository).remove(eq(favorite));
  }

  @Test
  void shouldDeleteArticle() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    String mutation = String.format(
        "mutation { deleteArticle(slug: \"%s\") { success } }",
        article.getSlug());

    Boolean success = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteArticle.success");

    assertThat(success).isTrue();
    verify(articleRepository).remove(eq(article));
  }

  @Test
  void shouldFailToUpdateArticleWithoutAuth() {
    SecurityContextHolder.clearContext();

    String mutation = String.format(
        "mutation { updateArticle(slug: \"%s\", changes: {title: \"Updated Title\"}) { article { slug } } }",
        article.getSlug());

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldFailToDeleteNonExistentArticle() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    String mutation = "mutation { deleteArticle(slug: \"non-existent\") { success } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }
}
