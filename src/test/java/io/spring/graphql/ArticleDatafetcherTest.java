package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(classes = {DgsAutoConfiguration.class, ArticleDatafetcher.class})
public class ArticleDatafetcherTest {

  @Autowired
  private DgsQueryExecutor dgsQueryExecutor;

  @MockBean
  private ArticleQueryService articleQueryService;

  @MockBean
  private UserRepository userRepository;

  private User user;
  private ArticleData articleData;

  @BeforeEach
  void setUp() {
    user = new User("user@example.com", "testuser", "password", "bio", "image");
    DateTime now = new DateTime();
    ProfileData profileData = new ProfileData(
        user.getId(),
        user.getUsername(),
        user.getBio(),
        user.getImage(),
        false);
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
        Arrays.asList("java", "spring"),
        profileData);

    AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
        "key", "anonymous", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldGetArticleBySlug() {
    when(articleQueryService.findBySlug(eq("test-article"), any()))
        .thenReturn(Optional.of(articleData));

    String query = "{ article(slug: \"test-article\") { slug title description body tagList } }";

    String slug = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.slug");
    String title = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.title");
    String description = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.description");
    String body = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.body");
    List<String> tagList = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.tagList");

    assertThat(slug).isEqualTo("test-article");
    assertThat(title).isEqualTo("Test Article");
    assertThat(description).isEqualTo("Test Description");
    assertThat(body).isEqualTo("Test Body");
    assertThat(tagList).containsExactly("java", "spring");
  }

  @Test
  void shouldReturnNullForNonExistentArticle() {
    when(articleQueryService.findBySlug(eq("non-existent"), any()))
        .thenReturn(Optional.empty());

    String query = "{ article(slug: \"non-existent\") { slug } }";

    Map<String, Object> result = dgsQueryExecutor.execute(query).getData();
    assertThat(result.get("article")).isNull();
  }

  @Test
  void shouldGetArticlesWithPagination() {
    CursorPager<ArticleData> cursorPager = new CursorPager<>(
        Arrays.asList(articleData),
        Direction.NEXT,
        false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query = "{ articles(first: 10) { edges { node { slug title } } pageInfo { hasNextPage hasPreviousPage } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");
    Boolean hasNextPage = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasNextPage");
    Boolean hasPreviousPage = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasPreviousPage");

    assertThat(edges).hasSize(1);
    assertThat(hasNextPage).isFalse();
    assertThat(hasPreviousPage).isFalse();
  }

  @Test
  void shouldGetArticlesFilteredByTag() {
    CursorPager<ArticleData> cursorPager = new CursorPager<>(
        Arrays.asList(articleData),
        Direction.NEXT,
        false);

    when(articleQueryService.findRecentArticlesWithCursor(eq("java"), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query = "{ articles(first: 10, withTag: \"java\") { edges { node { slug tagList } } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  void shouldGetArticlesFilteredByAuthor() {
    CursorPager<ArticleData> cursorPager = new CursorPager<>(
        Arrays.asList(articleData),
        Direction.NEXT,
        false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), eq("testuser"), any(), any(), any()))
        .thenReturn(cursorPager);

    String query = "{ articles(first: 10, authoredBy: \"testuser\") { edges { node { slug } } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  void shouldGetArticlesFilteredByFavoritedBy() {
    CursorPager<ArticleData> cursorPager = new CursorPager<>(
        Arrays.asList(articleData),
        Direction.NEXT,
        false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), eq("testuser"), any(), any()))
        .thenReturn(cursorPager);

    String query = "{ articles(first: 10, favoritedBy: \"testuser\") { edges { node { slug } } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  void shouldGetEmptyArticlesList() {
    CursorPager<ArticleData> cursorPager = new CursorPager<>(
        Arrays.asList(),
        Direction.NEXT,
        false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query = "{ articles(first: 10) { edges { node { slug } } } }";

    List<Map<String, Object>> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).isEmpty();
  }
}
