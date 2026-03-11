package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {DgsAutoConfiguration.class, ArticleDatafetcher.class})
public class ArticleDatafetcherTest extends GraphQLTestBase {

  @Autowired DgsQueryExecutor dgsQueryExecutor;

  @MockBean ArticleQueryService articleQueryService;

  @MockBean UserRepository userRepository;

  private ArticleData articleData;
  private ProfileData profileData;

  @BeforeEach
  @Override
  public void setUpUser() {
    super.setUpUser();
    profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    articleData =
        new ArticleData(
            "article-id",
            "test-slug",
            "Test Title",
            "Test Description",
            "Test Body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Arrays.asList("tag1", "tag2"),
            profileData);
  }

  @Test
  public void testFindArticleBySlug() {
    when(articleQueryService.findBySlug(eq("test-slug"), any()))
        .thenReturn(Optional.of(articleData));

    String query = "{ article(slug: \"test-slug\") { slug title description body favorited favoritesCount } }";
    String slug = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.slug");
    String title = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.title");

    assertEquals("test-slug", slug);
    assertEquals("Test Title", title);
  }

  @Test
  public void testFindArticleBySlugNotFound() {
    when(articleQueryService.findBySlug(eq("non-existent"), any()))
        .thenReturn(Optional.empty());

    String query = "{ article(slug: \"non-existent\") { slug title } }";
    
    assertThrows(Exception.class, () -> {
      dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.slug");
    });
  }

  @Test
  public void testGetArticles() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> pager = new CursorPager<>(articles, CursorPager.Direction.NEXT, true);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);

    String query = "{ articles(first: 10) { edges { node { slug title } } pageInfo { hasNextPage hasPreviousPage } } }";
    List<String> slugs = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges[*].node.slug");
    Boolean hasNextPage = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasNextPage");

    assertEquals(1, slugs.size());
    assertEquals("test-slug", slugs.get(0));
    assertEquals(true, hasNextPage);
  }

  @Test
  public void testGetArticlesWithFilters() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> pager = new CursorPager<>(articles, CursorPager.Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(eq("tag1"), eq("testuser"), any(), any(), any()))
        .thenReturn(pager);

    String query = "{ articles(first: 10, withTag: \"tag1\", authoredBy: \"testuser\") { edges { node { slug title } } } }";
    List<String> slugs = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges[*].node.slug");

    assertEquals(1, slugs.size());
    assertEquals("test-slug", slugs.get(0));
  }

  @Test
  public void testGetFeed() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> pager = new CursorPager<>(articles, CursorPager.Direction.NEXT, true);

    when(articleQueryService.findUserFeedWithCursor(any(), any()))
        .thenReturn(pager);

    String query = "{ feed(first: 10) { edges { node { slug title } } pageInfo { hasNextPage } } }";
    List<String> slugs = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.feed.edges[*].node.slug");
    Boolean hasNextPage = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.feed.pageInfo.hasNextPage");

    assertEquals(1, slugs.size());
    assertEquals("test-slug", slugs.get(0));
    assertEquals(true, hasNextPage);
  }

  @Test
  public void testGetArticlesRequiresFirstOrLast() {
    String query = "{ articles { edges { node { slug } } } }";
    
    assertThrows(Exception.class, () -> {
      dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles");
    });
  }
}
