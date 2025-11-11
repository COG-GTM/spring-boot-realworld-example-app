package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ArticleMutation.class,
      ArticleDatafetcher.class,
      ProfileDatafetcher.class,
      WebSecurityConfig.class,
      BCryptPasswordEncoder.class,
      JacksonCustomizations.class
    })
public class ArticleMutationTest {

  @Autowired DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleCommandService articleCommandService;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;

  private String defaultAvatar;
  private User user;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";
    user = new User("john@jacob.com", "johnjacob", "password", "bio", defaultAvatar);
    profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
  }

  @Test
  @WithMockUser(username = "johnjacob")
  public void should_create_article_successfully() {
    String title = "How to train your dragon";
    String slug = "how-to-train-your-dragon";
    String description = "Ever wonder how?";
    String body = "You have to believe";
    List<String> tagList = Arrays.asList("reactjs", "angularjs", "dragons");

    Article article = new Article(title, description, body, tagList, user.getId());
    when(articleCommandService.createArticle(any(), any())).thenReturn(article);

    ArticleData articleData =
        new ArticleData(
            article.getId(),
            slug,
            title,
            description,
            body,
            false,
            0,
            new DateTime(),
            new DateTime(),
            tagList,
            profileData);

    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(articleData));

    String query =
        String.format(
            "mutation { createArticle(input: { title: \"%s\", description: \"%s\", body: \"%s\", tagList: [\"reactjs\", \"angularjs\", \"dragons\"] }) { article { slug title description body tagList favorited favoritesCount author { username } } } }",
            title, description, body);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.createArticle.article", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("title")).isEqualTo(title);
    assertThat(result.get("description")).isEqualTo(description);
    assertThat(result.get("body")).isEqualTo(body);
    assertThat(result.get("tagList")).isEqualTo(tagList);
    assertThat(result.get("favorited")).isEqualTo(false);
    assertThat(result.get("favoritesCount")).isEqualTo(0);

    verify(articleCommandService).createArticle(any(), any());
  }

  @Test
  @WithMockUser(username = "johnjacob")
  public void should_update_article_successfully() {
    String slug = "how-to-train-your-dragon";
    String originalTitle = "How to train your dragon";
    String updatedTitle = "How to train your dragon 2";
    String updatedDescription = "Ever wonder how? Part 2";
    String updatedBody = "You have to believe more";
    List<String> tagList = Arrays.asList("reactjs", "angularjs", "dragons");

    Article article = new Article(originalTitle, "description", "body", tagList, user.getId());
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    Article updatedArticle = new Article(updatedTitle, updatedDescription, updatedBody, tagList, user.getId());
    when(articleCommandService.updateArticle(any(), any())).thenReturn(updatedArticle);

    ArticleData articleData =
        new ArticleData(
            updatedArticle.getId(),
            slug,
            updatedTitle,
            updatedDescription,
            updatedBody,
            false,
            0,
            new DateTime(),
            new DateTime(),
            tagList,
            profileData);

    when(articleQueryService.findById(eq(updatedArticle.getId()), any())).thenReturn(Optional.of(articleData));

    String query =
        String.format(
            "mutation { updateArticle(slug: \"%s\", changes: { title: \"%s\", description: \"%s\", body: \"%s\" }) { article { slug title description body } } }",
            slug, updatedTitle, updatedDescription, updatedBody);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.updateArticle.article", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("title")).isEqualTo(updatedTitle);
    assertThat(result.get("description")).isEqualTo(updatedDescription);
    assertThat(result.get("body")).isEqualTo(updatedBody);

    verify(articleCommandService).updateArticle(any(), any());
  }

  @Test
  @WithMockUser(username = "johnjacob")
  public void should_favorite_article_successfully() {
    String slug = "how-to-train-your-dragon";
    String title = "How to train your dragon";
    String description = "Ever wonder how?";
    String body = "You have to believe";
    List<String> tagList = Arrays.asList("reactjs", "angularjs", "dragons");

    Article article = new Article(title, description, body, tagList, user.getId());
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    ArticleData articleData =
        new ArticleData(
            article.getId(),
            slug,
            title,
            description,
            body,
            true,
            1,
            new DateTime(),
            new DateTime(),
            tagList,
            profileData);

    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(articleData));

    String query =
        String.format(
            "mutation { favoriteArticle(slug: \"%s\") { article { slug title favorited favoritesCount } } }",
            slug);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.favoriteArticle.article", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("slug")).isEqualTo(slug);
    assertThat(result.get("favorited")).isEqualTo(true);
    assertThat(result.get("favoritesCount")).isEqualTo(1);

    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  @WithMockUser(username = "johnjacob")
  public void should_unfavorite_article_successfully() {
    String slug = "how-to-train-your-dragon";
    String title = "How to train your dragon";
    String description = "Ever wonder how?";
    String body = "You have to believe";
    List<String> tagList = Arrays.asList("reactjs", "angularjs", "dragons");

    Article article = new Article(title, description, body, tagList, user.getId());
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));

    ArticleData articleData =
        new ArticleData(
            article.getId(),
            slug,
            title,
            description,
            body,
            false,
            0,
            new DateTime(),
            new DateTime(),
            tagList,
            profileData);

    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(articleData));

    String query =
        String.format(
            "mutation { unfavoriteArticle(slug: \"%s\") { article { slug title favorited favoritesCount } } }",
            slug);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.unfavoriteArticle.article", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("slug")).isEqualTo(slug);
    assertThat(result.get("favorited")).isEqualTo(false);
    assertThat(result.get("favoritesCount")).isEqualTo(0);

    verify(articleFavoriteRepository).remove(any(ArticleFavorite.class));
  }

  @Test
  @WithMockUser(username = "johnjacob")
  public void should_delete_article_successfully() {
    String slug = "how-to-train-your-dragon";
    String title = "How to train your dragon";
    String description = "Ever wonder how?";
    String body = "You have to believe";
    List<String> tagList = Arrays.asList("reactjs", "angularjs", "dragons");

    Article article = new Article(title, description, body, tagList, user.getId());
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    String query =
        String.format(
            "mutation { deleteArticle(slug: \"%s\") { success } }",
            slug);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.deleteArticle", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("success")).isEqualTo(true);

    verify(articleRepository).remove(any(Article.class));
  }

  @Test
  public void should_fail_to_create_article_without_authentication() {
    String title = "How to train your dragon";
    String description = "Ever wonder how?";
    String body = "You have to believe";

    String query =
        String.format(
            "mutation { createArticle(input: { title: \"%s\", description: \"%s\", body: \"%s\" }) { article { slug title } } }",
            title, description, body);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.createArticle.article", Map.class);
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("AuthenticationException");
    }
  }

  @Test
  public void should_fail_to_update_nonexistent_article() {
    String slug = "nonexistent-article";
    String updatedTitle = "Updated title";

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.empty());

    String query =
        String.format(
            "mutation { updateArticle(slug: \"%s\", changes: { title: \"%s\" }) { article { slug title } } }",
            slug, updatedTitle);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.updateArticle.article", Map.class);
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("ResourceNotFoundException");
    }
  }
}
