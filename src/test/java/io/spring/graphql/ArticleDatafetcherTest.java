package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.GraphQLTestWithCurrentUser;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.DateTimeCursor;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ArticleDatafetcherTest extends GraphQLTestWithCurrentUser {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;

  private ArticleData articleData;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
    ProfileData profileData = new ProfileData(user.getId(), "testuser", "", "", false);
    articleData = new ArticleData(
      "article-id",
      "test-slug",
      "Test Title",
      "Test Description",
      "Test Body",
      false,
      0,
      new DateTime(),
      new DateTime(),
      Arrays.asList("java", "spring"),
      profileData
    );
  }

  @Test
  public void should_query_article_by_slug() {
    when(articleQueryService.findBySlug(eq("test-slug"), any())).thenReturn(Optional.of(articleData));

    String query = "{ article(slug: \"test-slug\") { slug title description } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article", Collections.emptyMap());
    assertNotNull(result);
    assertEquals("test-slug", result.get("slug"));
    assertEquals("Test Title", result.get("title"));
  }

  @Test
  public void should_query_articles_with_pagination() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> pager = new CursorPager<>(
      articles,
      CursorPager.Direction.NEXT,
      true
    );
    
    when(articleQueryService.findRecentArticlesWithCursor(
      any(), any(), any(), any(CursorPageParameter.class), any()
    )).thenReturn(pager);

    String query = "{ articles(first: 10) { edges { node { slug title } } pageInfo { hasNextPage } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles", Collections.emptyMap());
    assertNotNull(result);
    
    List<Map<String, Object>> edges = (List<Map<String, Object>>) result.get("edges");
    assertFalse(edges.isEmpty());
  }

  @Test
  public void should_query_articles_with_tag_filter() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> pager = new CursorPager<>(
      articles,
      CursorPager.Direction.NEXT,
      false
    );
    
    when(articleQueryService.findRecentArticlesWithCursor(
      eq("java"), any(), any(), any(CursorPageParameter.class), any()
    )).thenReturn(pager);

    String query = "{ articles(first: 10, withTag: \"java\") { edges { node { slug title tagList } } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles", Collections.emptyMap());
    assertNotNull(result);
    
    List<Map<String, Object>> edges = (List<Map<String, Object>>) result.get("edges");
    assertFalse(edges.isEmpty());
  }

  @Test
  public void should_query_feed() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> pager = new CursorPager<>(
      articles,
      CursorPager.Direction.NEXT,
      true
    );
    
    when(articleQueryService.findUserFeedWithCursor(
      any(), any(CursorPageParameter.class)
    )).thenReturn(pager);

    String query = "{ feed(first: 10) { edges { node { slug title } } pageInfo { hasNextPage } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.feed", Collections.emptyMap());
    assertNotNull(result);
    
    List<Map<String, Object>> edges = (List<Map<String, Object>>) result.get("edges");
    assertFalse(edges.isEmpty());
  }
}
