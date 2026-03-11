package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.TypeRef;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ArticleDatafetcher.class,
      CommentDatafetcher.class,
      ProfileDatafetcher.class,
      WebSecurityConfig.class,
      JacksonCustomizations.class,
      GraphQLCustomizeExceptionHandler.class
    })
@Import({})
public class ArticleDatafetcherTest extends GraphQLTestBase {

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private CommentQueryService commentQueryService;

  @Test
  public void should_fetch_article_with_author() {
    String slug = "test-article";

    ArticleData articleData =
        new ArticleData(
            "article-id",
            slug,
            "Test Article",
            "Test Description",
            "Test Body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Arrays.asList("java"),
            new ProfileData(user.getId(), username, "Bio", defaultAvatar, false));

    when(articleQueryService.findBySlug(eq(slug), any())).thenReturn(Optional.of(articleData));

    String query =
        "query GetArticle($slug: String!) {"
            + "  article(slug: $slug) {"
            + "    slug"
            + "    title"
            + "    description"
            + "    body"
            + "    author {"
            + "      username"
            + "      bio"
            + "      image"
            + "      following"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", slug);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.article", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();
    assertThat(result.get("slug")).isEqualTo(slug);

    Map<String, Object> author = (Map<String, Object>) result.get("author");
    assertThat(author).isNotNull();
    assertThat(author.get("username")).isEqualTo(username);
  }

  @Test
  public void should_fetch_article_with_comments() {
    String slug = "test-article";

    ArticleData articleData =
        new ArticleData(
            "article-id",
            slug,
            "Test Article",
            "Test Description",
            "Test Body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Arrays.asList("java"),
            new ProfileData(user.getId(), username, "Bio", defaultAvatar, false));

    when(articleQueryService.findBySlug(eq(slug), any())).thenReturn(Optional.of(articleData));
    when(articleQueryService.findById(eq("article-id"), any()))
        .thenReturn(Optional.of(articleData));

    CommentData commentData =
        new CommentData(
            "comment-id",
            "Great article!",
            "article-id",
            new DateTime(),
            new DateTime(),
            new ProfileData(user.getId(), username, "Bio", defaultAvatar, false));

    List<CommentData> comments = Arrays.asList(commentData);
    CursorPager<CommentData> pager =
        new CursorPager<>(comments, CursorPager.Direction.NEXT, false);

    when(commentQueryService.findByArticleIdWithCursor(
            eq("article-id"), any(), any(CursorPageParameter.class)))
        .thenReturn(pager);

    String query =
        "query GetArticle($slug: String!) {"
            + "  article(slug: $slug) {"
            + "    slug"
            + "    title"
            + "    comments(first: 10) {"
            + "      edges {"
            + "        node {"
            + "          id"
            + "          body"
            + "          author {"
            + "            username"
            + "          }"
            + "        }"
            + "      }"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", slug);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.article", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();
    assertThat(result.get("comments")).isNotNull();
  }

  @Test
  public void should_fetch_articles_with_pagination_info() {
    ArticleData articleData1 =
        new ArticleData(
            "article-1",
            "test-article-1",
            "Test Article 1",
            "Description 1",
            "Body 1",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Arrays.asList("java"),
            new ProfileData(user.getId(), username, "", defaultAvatar, false));

    ArticleData articleData2 =
        new ArticleData(
            "article-2",
            "test-article-2",
            "Test Article 2",
            "Description 2",
            "Body 2",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Arrays.asList("spring"),
            new ProfileData(user.getId(), username, "", defaultAvatar, false));

    List<ArticleData> articles = Arrays.asList(articleData1, articleData2);
    CursorPager<ArticleData> pager =
        new CursorPager<>(articles, CursorPager.Direction.NEXT, true);

    when(articleQueryService.findRecentArticlesWithCursor(
            any(), any(), any(), any(CursorPageParameter.class), any()))
        .thenReturn(pager);

    String query =
        "query GetArticles($first: Int) {"
            + "  articles(first: $first) {"
            + "    edges {"
            + "      cursor"
            + "      node {"
            + "        slug"
            + "        title"
            + "      }"
            + "    }"
            + "    pageInfo {"
            + "      hasNextPage"
            + "      hasPreviousPage"
            + "      startCursor"
            + "      endCursor"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("first", 10);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.articles", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();

    Map<String, Object> pageInfo = (Map<String, Object>) result.get("pageInfo");
    assertThat(pageInfo).isNotNull();
    assertThat(pageInfo.get("hasNextPage")).isEqualTo(true);
    assertThat(pageInfo.get("hasPreviousPage")).isEqualTo(false);
  }

  @Test
  public void should_fetch_favorited_articles() {
    authenticateUser();

    ArticleData articleData =
        new ArticleData(
            "article-1",
            "test-article-1",
            "Test Article 1",
            "Description 1",
            "Body 1",
            true,
            5,
            new DateTime(),
            new DateTime(),
            Arrays.asList("java"),
            new ProfileData(user.getId(), username, "", defaultAvatar, false));

    when(articleQueryService.findBySlug(eq("test-article-1"), any()))
        .thenReturn(Optional.of(articleData));

    String query =
        "query GetArticle($slug: String!) {"
            + "  article(slug: $slug) {"
            + "    slug"
            + "    title"
            + "    favorited"
            + "    favoritesCount"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", "test-article-1");

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.article", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();
    assertThat(result.get("favorited")).isEqualTo(true);
    assertThat(result.get("favoritesCount")).isEqualTo(5);

    clearAuthentication();
  }
}
