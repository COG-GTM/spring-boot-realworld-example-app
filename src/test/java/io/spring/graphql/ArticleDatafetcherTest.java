package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.ArrayList;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.netflix.graphql.dgs.exceptions.QueryException;

@SpringBootTest
public class ArticleDatafetcherTest {

  @Autowired
  private DgsQueryExecutor dgsQueryExecutor;

  @MockBean
  private ArticleQueryService articleQueryService;

  @MockBean
  private UserRepository userRepository;

  @MockBean
  private io.spring.application.ProfileQueryService profileQueryService;

  private User user;
  private ArticleData articleData;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    profileData = new ProfileData(user.getId(), "testuser", "bio", "image", false);
    articleData = new ArticleData(
        "article-id",
        "test-article",
        "Test Article",
        "Test Description",
        "Test Body",
        false,
        5,
        new DateTime(),
        new DateTime(),
        Arrays.asList("tag1", "tag2"),
        profileData
    );

    setAnonymousAuthentication();
  }

  private void setAnonymousAuthentication() {
    AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
        "key", "anonymousUser",
        java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);
  }

  @Test
  public void should_get_article_by_slug() {
    when(articleQueryService.findBySlug(eq("test-article"), any()))
        .thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ article(slug: \"test-article\") { slug title description body favorited favoritesCount tagList author { username bio image following } } }";

    String slug = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.slug");
    String title = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.title");
    String description = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.description");
    String body = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.body");
    Boolean favorited = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.favorited");
    Integer favoritesCount = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.favoritesCount");
    List<String> tagList = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.tagList");

    assertThat(slug).isEqualTo("test-article");
    assertThat(title).isEqualTo("Test Article");
    assertThat(description).isEqualTo("Test Description");
    assertThat(body).isEqualTo("Test Body");
    assertThat(favorited).isFalse();
    assertThat(favoritesCount).isEqualTo(5);
    assertThat(tagList).containsExactly("tag1", "tag2");
  }

  @Test
  public void should_get_article_with_authenticated_user_favorited_status() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>()));

    ArticleData favoritedArticle = new ArticleData(
        "article-id",
        "test-article",
        "Test Article",
        "Test Description",
        "Test Body",
        true,
        10,
        new DateTime(),
        new DateTime(),
        Arrays.asList("tag1"),
        profileData
    );

    when(articleQueryService.findBySlug(eq("test-article"), eq(user)))
        .thenReturn(Optional.of(favoritedArticle));
    when(profileQueryService.findByUsername(eq("testuser"), eq(user)))
        .thenReturn(Optional.of(profileData));

    String query = "{ article(slug: \"test-article\") { slug favorited favoritesCount } }";

    Boolean favorited = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.favorited");
    Integer favoritesCount = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.favoritesCount");

    assertThat(favorited).isTrue();
    assertThat(favoritesCount).isEqualTo(10);
  }

  @Test
  public void should_return_error_for_non_existent_article() {
    when(articleQueryService.findBySlug(eq("non-existent"), any()))
        .thenReturn(Optional.empty());

    String query = "{ article(slug: \"non-existent\") { slug } }";

    assertThrows(QueryException.class, () -> {
      dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.slug");
    });
  }

  @Test
  public void should_get_articles_list_with_first_pagination() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, true);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ articles(first: 10) { edges { cursor node { slug title } } pageInfo { hasNextPage hasPreviousPage startCursor endCursor } } }";

    Boolean hasNextPage = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasNextPage");
    Boolean hasPreviousPage = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasPreviousPage");
    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(hasNextPage).isTrue();
    assertThat(hasPreviousPage).isFalse();
    assertThat(edges).hasSize(1);
  }

  @Test
  public void should_get_articles_list_with_last_pagination() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.PREV, true);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ articles(last: 10) { edges { cursor node { slug title } } pageInfo { hasNextPage hasPreviousPage } } }";

    Boolean hasNextPage = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasNextPage");
    Boolean hasPreviousPage = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasPreviousPage");

    assertThat(hasNextPage).isFalse();
    assertThat(hasPreviousPage).isTrue();
  }

  @Test
  public void should_filter_articles_by_author() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), eq("testuser"), any(), any(), any()))
        .thenReturn(cursorPager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ articles(first: 10, authoredBy: \"testuser\") { edges { node { slug author { username } } } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  public void should_filter_articles_by_favorited_by() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), eq("favoriter"), any(), any()))
        .thenReturn(cursorPager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ articles(first: 10, favoritedBy: \"favoriter\") { edges { node { slug } } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  public void should_filter_articles_by_tag() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(eq("tag1"), any(), any(), any(), any()))
        .thenReturn(cursorPager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ articles(first: 10, withTag: \"tag1\") { edges { node { slug tagList } } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  public void should_filter_articles_with_combined_filters() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(eq("tag1"), eq("testuser"), eq("favoriter"), any(), any()))
        .thenReturn(cursorPager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ articles(first: 10, withTag: \"tag1\", authoredBy: \"testuser\", favoritedBy: \"favoriter\") { edges { node { slug } } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  public void should_get_feed_for_authenticated_user() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>()));

    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);

    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(cursorPager);
    when(profileQueryService.findByUsername(eq("testuser"), eq(user)))
        .thenReturn(Optional.of(profileData));

    String query = "{ feed(first: 10) { edges { node { slug title } } pageInfo { hasNextPage hasPreviousPage } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.feed.edges");
    Boolean hasNextPage = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.feed.pageInfo.hasNextPage");

    assertThat(edges).hasSize(1);
    assertThat(hasNextPage).isFalse();
  }

  @Test
  public void should_get_empty_feed_for_unauthenticated_user() {
    CursorPager<ArticleData> cursorPager = new CursorPager<>(new ArrayList<>(), Direction.NEXT, false);

    when(articleQueryService.findUserFeedWithCursor(any(), any()))
        .thenReturn(cursorPager);

    String query = "{ feed(first: 10) { edges { node { slug } } pageInfo { hasNextPage hasPreviousPage } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.feed.edges");

    assertThat(edges).isEmpty();
  }

  @Test
  public void should_paginate_articles_with_after_cursor() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ articles(first: 10, after: \"1672531200000\") { edges { cursor node { slug } } pageInfo { hasNextPage startCursor endCursor } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  public void should_paginate_articles_with_before_cursor() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.PREV, false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ articles(last: 10, before: \"1704067199000\") { edges { cursor node { slug } } pageInfo { hasPreviousPage startCursor endCursor } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  public void should_return_correct_page_info_for_empty_results() {
    CursorPager<ArticleData> cursorPager = new CursorPager<>(new ArrayList<>(), Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query = "{ articles(first: 10) { edges { node { slug } } pageInfo { hasNextPage hasPreviousPage startCursor endCursor } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");
    Boolean hasNextPage = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasNextPage");
    Boolean hasPreviousPage = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasPreviousPage");

    assertThat(edges).isEmpty();
    assertThat(hasNextPage).isFalse();
    assertThat(hasPreviousPage).isFalse();
  }

  @Test
  public void should_get_feed_with_last_pagination() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>()));

    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.PREV, true);

    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(cursorPager);
    when(profileQueryService.findByUsername(eq("testuser"), eq(user)))
        .thenReturn(Optional.of(profileData));

    String query = "{ feed(last: 10) { edges { node { slug } } pageInfo { hasNextPage hasPreviousPage } } }";

    Boolean hasPreviousPage = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.feed.pageInfo.hasPreviousPage");

    assertThat(hasPreviousPage).isTrue();
  }
}
