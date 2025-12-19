package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.ProfileQueryService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class ArticleDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private UserRepository userRepository;

  @MockBean private ProfileQueryService profileQueryService;

  private User user;
  private ArticleData articleData;
  private ProfileData profileData;

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

    profileData =
        new ProfileData(
            user.getId(),
            user.getUsername(),
            user.getBio(),
            user.getImage(),
            false);

    DateTime now = new DateTime();
    articleData =
        new ArticleData(
            "article-id",
            "test-title",
            "Test Title",
            "Test Description",
            "Test Body",
            false,
            0,
            now,
            now,
            Arrays.asList("tag1", "tag2"),
            profileData);
  }

  @Test
  public void should_get_article_by_slug() {
    when(articleQueryService.findBySlug(eq("test-title"), any()))
        .thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData));

    String query =
        "query { article(slug: \"test-title\") "
            + "{ slug title description body tagList } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article");
    assertNotNull(result);
    assertEquals("test-title", result.get("slug"));
    assertEquals("Test Title", result.get("title"));
  }

  @Test
  public void should_return_null_for_nonexistent_article() {
    when(articleQueryService.findBySlug(eq("nonexistent"), any()))
        .thenReturn(Optional.empty());

    String query =
        "query { article(slug: \"nonexistent\") { slug title } }";

    assertThrows(
        Exception.class,
        () -> {
          dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article");
        });
  }

  @Test
  public void should_get_articles_list() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(
            Arrays.asList(articleData),
            CursorPager.Direction.NEXT,
            false);
    when(articleQueryService.findRecentArticlesWithCursor(
            any(), any(), any(), any(), any()))
        .thenReturn(pager);
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData));

    String query =
        "query { articles(first: 10) { "
            + "edges { cursor node { slug title } } "
            + "pageInfo { hasNextPage hasPreviousPage } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles");
    assertNotNull(result);
    assertNotNull(result.get("edges"));
  }

  @Test
  public void should_get_articles_filtered_by_tag() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(
            Arrays.asList(articleData),
            CursorPager.Direction.NEXT,
            false);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("tag1"), any(), any(), any(), any()))
        .thenReturn(pager);
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData));

    String query =
        "query { articles(first: 10, withTag: \"tag1\") "
            + "{ edges { node { slug tagList } } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles");
    assertNotNull(result);
  }

  @Test
  public void should_get_articles_filtered_by_author() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(
            Arrays.asList(articleData),
            CursorPager.Direction.NEXT,
            false);
    when(articleQueryService.findRecentArticlesWithCursor(
            any(), eq("testuser"), any(), any(), any()))
        .thenReturn(pager);
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData));

    String query =
        "query { articles(first: 10, authoredBy: \"testuser\") "
            + "{ edges { node { slug } } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles");
    assertNotNull(result);
  }

  @Test
  public void should_get_articles_filtered_by_favorited() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(
            Arrays.asList(articleData),
            CursorPager.Direction.NEXT,
            false);
    when(articleQueryService.findRecentArticlesWithCursor(
            any(), any(), eq("testuser"), any(), any()))
        .thenReturn(pager);
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData));

    String query =
        "query { articles(first: 10, favoritedBy: \"testuser\") "
            + "{ edges { node { slug } } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles");
    assertNotNull(result);
  }

  @Test
  public void should_get_feed() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(
            Arrays.asList(articleData),
            CursorPager.Direction.NEXT,
            false);
    when(articleQueryService.findUserFeedWithCursor(any(), any()))
        .thenReturn(pager);
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData));

    String query =
        "query { feed(first: 10) { "
            + "edges { cursor node { slug title } } "
            + "pageInfo { hasNextPage hasPreviousPage } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.feed");
    assertNotNull(result);
  }

  @Test
  public void should_get_empty_articles_list() {
    CursorPager<ArticleData> emptyPager =
        new CursorPager<>(
            Collections.emptyList(),
            CursorPager.Direction.NEXT,
            false);
    when(articleQueryService.findRecentArticlesWithCursor(
            any(), any(), any(), any(), any()))
        .thenReturn(emptyPager);

    String query =
        "query { articles(first: 10) { edges { node { slug } } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles");
    assertNotNull(result);
    List<?> edges = (List<?>) result.get("edges");
    assertTrue(edges.isEmpty());
  }

  @Test
  public void should_get_article_with_author_profile() {
    when(articleQueryService.findBySlug(eq("test-title"), any()))
        .thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData));

    String query =
        "query { article(slug: \"test-title\") "
            + "{ slug title author { username bio following } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article");
    assertNotNull(result);
    Map<String, Object> author = (Map<String, Object>) result.get("author");
    assertNotNull(author);
    assertEquals("testuser", author.get("username"));
  }

  @Test
  public void should_handle_pagination_with_has_next() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(
            Arrays.asList(articleData),
            CursorPager.Direction.NEXT,
            true);
    when(articleQueryService.findRecentArticlesWithCursor(
            any(), any(), any(), any(), any()))
        .thenReturn(pager);
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData));

    String query =
        "query { articles(first: 10) { "
            + "edges { cursor node { slug } } "
            + "pageInfo { hasNextPage hasPreviousPage } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles");
    assertNotNull(result);
    Map<String, Object> pageInfo =
        (Map<String, Object>) result.get("pageInfo");
    assertNotNull(pageInfo);
    assertTrue((Boolean) pageInfo.get("hasNextPage"));
  }

  @Test
  public void should_handle_backward_pagination() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(
            Arrays.asList(articleData),
            CursorPager.Direction.PREV,
            true);
    when(articleQueryService.findRecentArticlesWithCursor(
            any(), any(), any(), any(), any()))
        .thenReturn(pager);
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData));

    String query =
        "query { articles(last: 10) { "
            + "edges { cursor node { slug } } "
            + "pageInfo { hasNextPage hasPreviousPage } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles");
    assertNotNull(result);
    Map<String, Object> pageInfo =
        (Map<String, Object>) result.get("pageInfo");
    assertNotNull(pageInfo);
    assertTrue((Boolean) pageInfo.get("hasPreviousPage"));
  }
}
