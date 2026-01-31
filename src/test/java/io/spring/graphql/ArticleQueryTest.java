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
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
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
      WebSecurityConfig.class,
      JacksonCustomizations.class,
      GraphQLCustomizeExceptionHandler.class
    })
@Import({})
public class ArticleQueryTest extends GraphQLTestBase {

  @MockBean private ArticleQueryService articleQueryService;

  @Test
  public void should_query_article_by_slug() {
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
            Arrays.asList("java", "spring"),
            new ProfileData(user.getId(), username, "", defaultAvatar, false));

    when(articleQueryService.findBySlug(eq(slug), any())).thenReturn(Optional.of(articleData));

    String query =
        "query GetArticle($slug: String!) {"
            + "  article(slug: $slug) {"
            + "    slug"
            + "    title"
            + "    description"
            + "    body"
            + "    tagList"
            + "    favorited"
            + "    favoritesCount"
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
    assertThat(result.get("title")).isEqualTo("Test Article");
    assertThat(result.get("description")).isEqualTo("Test Description");
    assertThat(result.get("body")).isEqualTo("Test Body");
  }

  @Test
  public void should_query_articles_with_pagination() {
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
        new CursorPager<>(articles, CursorPager.Direction.NEXT, false);

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
            + "        description"
            + "        author {"
            + "          username"
            + "        }"
            + "      }"
            + "    }"
            + "    pageInfo {"
            + "      hasNextPage"
            + "      hasPreviousPage"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("first", 10);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.articles", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();
    assertThat(result.get("edges")).isNotNull();
  }

  @Test
  public void should_query_articles_with_tag_filter() {
    String tag = "java";

    ArticleData articleData =
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
            Arrays.asList(tag),
            new ProfileData(user.getId(), username, "", defaultAvatar, false));

    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> pager =
        new CursorPager<>(articles, CursorPager.Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(
            eq(tag), any(), any(), any(CursorPageParameter.class), any()))
        .thenReturn(pager);

    String query =
        "query GetArticles($first: Int, $withTag: String) {"
            + "  articles(first: $first, withTag: $withTag) {"
            + "    edges {"
            + "      node {"
            + "        slug"
            + "        title"
            + "        tagList"
            + "      }"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("first", 10);
    variables.put("withTag", tag);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.articles", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();
  }

  @Test
  public void should_query_articles_by_author() {
    String authorUsername = "testauthor";

    ArticleData articleData =
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
            new ProfileData(user.getId(), authorUsername, "", defaultAvatar, false));

    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> pager =
        new CursorPager<>(articles, CursorPager.Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(
            any(), eq(authorUsername), any(), any(CursorPageParameter.class), any()))
        .thenReturn(pager);

    String query =
        "query GetArticles($first: Int, $authoredBy: String) {"
            + "  articles(first: $first, authoredBy: $authoredBy) {"
            + "    edges {"
            + "      node {"
            + "        slug"
            + "        title"
            + "        author {"
            + "          username"
            + "        }"
            + "      }"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("first", 10);
    variables.put("authoredBy", authorUsername);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.articles", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();
  }

  @Test
  public void should_query_user_feed() {
    authenticateUser();

    ArticleData articleData =
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

    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> pager =
        new CursorPager<>(articles, CursorPager.Direction.NEXT, false);

    when(articleQueryService.findUserFeedWithCursor(any(), any(CursorPageParameter.class)))
        .thenReturn(pager);

    String query =
        "query GetFeed($first: Int) {"
            + "  feed(first: $first) {"
            + "    edges {"
            + "      node {"
            + "        slug"
            + "        title"
            + "        author {"
            + "          username"
            + "        }"
            + "      }"
            + "    }"
            + "    pageInfo {"
            + "      hasNextPage"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("first", 10);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.feed", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();

    clearAuthentication();
  }
}
