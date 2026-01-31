package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
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

@SpringBootTest
public class ArticleDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;

  private User user;
  private ArticleData articleData;

  @BeforeEach
  public void setUp() {
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
    DateTime now = new DateTime();
    ProfileData profileData =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    articleData =
        new ArticleData(
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
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_get_article_by_slug() {
    when(articleQueryService.findBySlug(eq("test-article"), any()))
        .thenReturn(Optional.of(articleData));

    String query =
        "{ article(slug: \"test-article\") { slug title description body tagList favoritesCount } }";

    String slug = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.slug");
    String title = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.title");

    assertThat(slug).isEqualTo("test-article");
    assertThat(title).isEqualTo("Test Article");
  }

  @Test
  public void should_get_articles_with_pagination() {
    CursorPager<ArticleData> cursorPager =
        new CursorPager<>(Arrays.asList(articleData), Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query = "{ articles(first: 10) { edges { node { slug title } } pageInfo { hasNextPage hasPreviousPage } } }";

    List<Map<String, Object>> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  public void should_get_articles_filtered_by_tag() {
    CursorPager<ArticleData> cursorPager =
        new CursorPager<>(Arrays.asList(articleData), Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query = "{ articles(first: 10, withTag: \"java\") { edges { node { slug tagList } } } }";

    List<Map<String, Object>> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  public void should_get_articles_filtered_by_author() {
    CursorPager<ArticleData> cursorPager =
        new CursorPager<>(Arrays.asList(articleData), Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(
            any(), eq("testuser"), any(), any(), any()))
        .thenReturn(cursorPager);

    String query =
        "{ articles(first: 10, authoredBy: \"testuser\") { edges { node { slug } } } }";

    List<Map<String, Object>> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  public void should_get_empty_articles_list() {
    CursorPager<ArticleData> cursorPager =
        new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query = "{ articles(first: 10) { edges { node { slug } } } }";

    List<Map<String, Object>> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).isEmpty();
  }
}
