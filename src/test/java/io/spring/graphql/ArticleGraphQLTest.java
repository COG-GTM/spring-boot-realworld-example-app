package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.TestHelper;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.ProfileQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {DgsAutoConfiguration.class, ArticleDatafetcher.class, ArticleMutation.class, ProfileDatafetcher.class, CommentDatafetcher.class})
@ActiveProfiles("test")
public class ArticleGraphQLTest extends GraphQLTestBase {

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private ArticleCommandService articleCommandService;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;

  @MockBean private ProfileQueryService profileQueryService;

  @MockBean private CommentQueryService commentQueryService;

  @Test
  void should_query_article_by_slug() {
    setAuthenticatedUser(user);

    String slug = "test-article";
    DateTime time = new DateTime();
    Article article = new Article("Test Article", "Description", "Body", Arrays.asList("java", "spring"), user.getId(), time);
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);

    when(articleQueryService.findBySlug(eq(slug), any())).thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(username), any())).thenReturn(Optional.of(articleData.getProfileData()));

    String query =
        "query { article(slug: \"" + slug + "\") { slug title description body favorited favoritesCount tagList author { username } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article");

    assertThat(result.get("slug")).isEqualTo(articleData.getSlug());
    assertThat(result.get("title")).isEqualTo(articleData.getTitle());
    assertThat(result.get("description")).isEqualTo(articleData.getDescription());
    assertThat(result.get("body")).isEqualTo(articleData.getBody());
  }

  @Test
  void should_query_articles_with_pagination() {
    setAuthenticatedUser(user);

    ArticleData articleData1 = TestHelper.articleDataFixture("1", user);
    ArticleData articleData2 = TestHelper.articleDataFixture("2", user);
    List<ArticleData> articles = Arrays.asList(articleData1, articleData2);

    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, true);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query =
        "query { articles(first: 10) { edges { cursor node { slug title } } pageInfo { hasNextPage hasPreviousPage } } }";

    List<Map<String, Object>> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(2);
  }

  @Test
  void should_query_articles_with_tag_filter() {
    setAuthenticatedUser(user);

    ArticleData articleData = TestHelper.articleDataFixture("1", user);
    List<ArticleData> articles = Collections.singletonList(articleData);

    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(eq("java"), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query =
        "query { articles(first: 10, withTag: \"java\") { edges { node { slug } } } }";

    List<Map<String, Object>> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  void should_query_articles_by_author() {
    setAuthenticatedUser(user);

    ArticleData articleData = TestHelper.articleDataFixture("1", user);
    List<ArticleData> articles = Collections.singletonList(articleData);

    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), eq(username), any(), any(), any()))
        .thenReturn(cursorPager);

    String query =
        "query { articles(first: 10, authoredBy: \"" + username + "\") { edges { node { slug } } } }";

    List<Map<String, Object>> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  void should_query_articles_favorited_by_user() {
    setAuthenticatedUser(user);

    ArticleData articleData = TestHelper.articleDataFixture("1", user);
    List<ArticleData> articles = Collections.singletonList(articleData);

    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), eq(username), any(), any()))
        .thenReturn(cursorPager);

    String query =
        "query { articles(first: 10, favoritedBy: \"" + username + "\") { edges { node { slug } } } }";

    List<Map<String, Object>> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  void should_create_article_when_authenticated() {
    setAuthenticatedUser(user);

    String title = "New Article";
    String description = "Article Description";
    String body = "Article Body";
    List<String> tagList = Arrays.asList("java", "spring");

    Article article = new Article(title, description, body, tagList, user.getId());
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);

    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { createArticle(input: { title: \"" + title + "\", description: \"" + description + "\", body: \"" + body + "\", tagList: [\"java\", \"spring\"] }) { article { slug title description body tagList } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createArticle.article");

    assertThat(result.get("title")).isEqualTo(title);
    assertThat(result.get("description")).isEqualTo(description);
    assertThat(result.get("body")).isEqualTo(body);
  }

  @Test
  void should_update_article_when_author() {
    setAuthenticatedUser(user);

    String slug = "existing-article";
    String newTitle = "Updated Title";
    String newDescription = "Updated Description";
    String newBody = "Updated Body";

    Article originalArticle = new Article("Original Title", "Original Desc", "Original Body", Collections.emptyList(), user.getId());
    Article updatedArticle = new Article(newTitle, newDescription, newBody, Collections.emptyList(), user.getId());
    ArticleData updatedArticleData = TestHelper.getArticleDataFromArticleAndUser(updatedArticle, user);

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(originalArticle));
    when(articleCommandService.updateArticle(eq(originalArticle), any())).thenReturn(updatedArticle);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(updatedArticleData));

    String mutation =
        "mutation { updateArticle(slug: \"" + slug + "\", changes: { title: \"" + newTitle + "\", description: \"" + newDescription + "\", body: \"" + newBody + "\" }) { article { title description body } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateArticle.article");

    assertThat(result.get("title")).isEqualTo(newTitle);
    assertThat(result.get("description")).isEqualTo(newDescription);
    assertThat(result.get("body")).isEqualTo(newBody);
  }

  @Test
  void should_favorite_article_when_authenticated() {
    setAuthenticatedUser(user);

    String slug = "article-to-favorite";
    Article article = new Article("Title", "Desc", "Body", Collections.emptyList(), user.getId());
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation = "mutation { favoriteArticle(slug: \"" + slug + "\") { article { slug favorited } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.favoriteArticle.article");

    assertThat(result.get("slug")).isEqualTo(articleData.getSlug());
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  void should_unfavorite_article_when_authenticated() {
    setAuthenticatedUser(user);

    String slug = "article-to-unfavorite";
    Article article = new Article("Title", "Desc", "Body", Collections.emptyList(), user.getId());
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId()))).thenReturn(Optional.of(favorite));
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation = "mutation { unfavoriteArticle(slug: \"" + slug + "\") { article { slug } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfavoriteArticle.article");

    assertThat(result.get("slug")).isEqualTo(articleData.getSlug());
    verify(articleFavoriteRepository).remove(eq(favorite));
  }

  @Test
  void should_delete_article_when_author() {
    setAuthenticatedUser(user);

    String slug = "article-to-delete";
    Article article = new Article("Title", "Desc", "Body", Collections.emptyList(), user.getId());

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    String mutation = "mutation { deleteArticle(slug: \"" + slug + "\") { success } }";

    Boolean success = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteArticle.success");

    assertThat(success).isTrue();
    verify(articleRepository).remove(eq(article));
  }

  @Test
  void should_query_user_feed_when_authenticated() {
    setAuthenticatedUser(user);

    ArticleData articleData = TestHelper.articleDataFixture("1", user);
    List<ArticleData> articles = Collections.singletonList(articleData);

    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);

    when(articleQueryService.findUserFeedWithCursor(eq(user), any(CursorPageParameter.class)))
        .thenReturn(cursorPager);

    String query = "query { feed(first: 10) { edges { node { slug title } } } }";

    List<Map<String, Object>> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.feed.edges");

    assertThat(edges).hasSize(1);
  }
}
