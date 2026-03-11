package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {DgsAutoConfiguration.class, ArticleDatafetcher.class})
@Import({ProfileDatafetcher.class, CommentDatafetcher.class})
@ActiveProfiles("test")
public class ArticleDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private UserRepository userRepository;

  @MockBean private io.spring.application.ProfileQueryService profileQueryService;

  @MockBean private io.spring.application.CommentQueryService commentQueryService;

  private User user;
  private ArticleData articleData;
  private ProfileData profileData;

  @BeforeEach
  void setUp() {
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    DateTime now = DateTime.now();
    articleData = new ArticleData(
        "article-id",
        "test-article",
        "Test Article",
        "Test Description",
        "Test Body",
        false,
        0,
        now,
        now,
        Arrays.asList("tag1", "tag2"),
        profileData);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, null));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldGetArticleBySlug() {
    when(articleQueryService.findBySlug(eq("test-article"), any()))
        .thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ article(slug: \"test-article\") { slug title description body tagList favorited favoritesCount } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article");

    assertThat(result.get("slug")).isEqualTo("test-article");
    assertThat(result.get("title")).isEqualTo("Test Article");
    assertThat(result.get("description")).isEqualTo("Test Description");
    assertThat(result.get("body")).isEqualTo("Test Body");
    assertThat((List<String>) result.get("tagList")).containsExactly("tag1", "tag2");
    assertThat(result.get("favorited")).isEqualTo(false);
    assertThat(result.get("favoritesCount")).isEqualTo(0);
  }

  @Test
  void shouldReturnErrorWhenArticleNotFound() {
    when(articleQueryService.findBySlug(eq("non-existent"), any()))
        .thenReturn(Optional.empty());

    String query = "{ article(slug: \"non-existent\") { slug title } }";

    var result = dgsQueryExecutor.execute(query);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldGetArticlesWithPagination() {
    CursorPager<ArticleData> pager = new CursorPager<>(
        Collections.singletonList(articleData),
        Direction.NEXT,
        true);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ articles(first: 10) { edges { node { slug title } cursor } pageInfo { hasNextPage hasPreviousPage } } }";

    Map<String, Object> pageInfo = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo");

    assertThat(pageInfo.get("hasNextPage")).isEqualTo(true);
    assertThat(pageInfo.get("hasPreviousPage")).isEqualTo(false);
  }

  @Test
  void shouldGetArticlesFilteredByTag() {
    CursorPager<ArticleData> pager = new CursorPager<>(
        Collections.singletonList(articleData),
        Direction.NEXT,
        false);

    when(articleQueryService.findRecentArticlesWithCursor(eq("tag1"), any(), any(), any(), any()))
        .thenReturn(pager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ articles(first: 10, withTag: \"tag1\") { edges { node { slug title tagList } } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
    assertThat(edges.get(0).get("node")).isNotNull();
  }

  @Test
  void shouldGetArticlesFilteredByAuthor() {
    CursorPager<ArticleData> pager = new CursorPager<>(
        Collections.singletonList(articleData),
        Direction.NEXT,
        false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), eq("testuser"), any(), any(), any()))
        .thenReturn(pager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ articles(first: 10, authoredBy: \"testuser\") { edges { node { slug title } } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  void shouldGetArticlesFilteredByFavoritedBy() {
    CursorPager<ArticleData> pager = new CursorPager<>(
        Collections.singletonList(articleData),
        Direction.NEXT,
        false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), eq("testuser"), any(), any()))
        .thenReturn(pager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ articles(first: 10, favoritedBy: \"testuser\") { edges { node { slug title } } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  void shouldReturnErrorWhenFirstAndLastBothNull() {
    String query = "{ articles { edges { node { slug } } } }";

    var result = dgsQueryExecutor.execute(query);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldGetArticlesWithLastPagination() {
    CursorPager<ArticleData> pager = new CursorPager<>(
        Collections.singletonList(articleData),
        Direction.PREV,
        true);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ articles(last: 10) { edges { node { slug title } } pageInfo { hasNextPage hasPreviousPage } } }";

    Map<String, Object> pageInfo = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo");

    assertThat(pageInfo.get("hasNextPage")).isEqualTo(false);
    assertThat(pageInfo.get("hasPreviousPage")).isEqualTo(true);
  }
}
