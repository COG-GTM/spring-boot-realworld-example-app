package io.spring.graphql;

import com.jayway.jsonpath.JsonPath;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.bookmark.ArticleBookmark;
import io.spring.core.bookmark.ArticleBookmarkRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.request.ServletWebRequest;

/** Covers docs/specs/article-bookmarks.md AC-9, AC-12..AC-16 through the DGS GraphQL schema. */
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite:file:bookmarks_graphql_test?mode=memory&cache=shared"
    })
public class ArticleBookmarkGraphQLTest {
  private static final String BOOKMARK =
      "mutation($slug: String!) { bookmarkArticle(slug: $slug) {"
          + " article { slug bookmarked favorited favoritesCount } } }";
  private static final String UNBOOKMARK =
      "mutation($slug: String!) { unbookmarkArticle(slug: $slug) {"
          + " article { slug bookmarked } } }";
  private static final String ARTICLE =
      "query($slug: String!) { article(slug: $slug) { slug bookmarked } }";
  private static final String MY_BOOKMARKS =
      "query($first: Int, $after: String) { me { username bookmarks(first: $first, after: $after) {"
          + " edges { cursor node { slug bookmarked author { username } } }"
          + " pageInfo { hasNextPage endCursor } } } }";

  @Autowired private DgsQueryExecutor dgsQueryExecutor;
  @Autowired private UserRepository userRepository;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private ArticleBookmarkRepository articleBookmarkRepository;

  private User reader;
  private User author;
  private Article article;

  @BeforeEach
  public void setUp() {
    String seed = UUID.randomUUID().toString().substring(0, 8);
    reader = new User(seed + "reader@test.com", seed + "reader", "123", "", "");
    author = new User(seed + "author@test.com", seed + "author", "123", "", "");
    userRepository.save(reader);
    userRepository.save(author);
    article = article("graphql " + seed, new DateTime().minusHours(1));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private Article article(String title, DateTime createdAt) {
    Article a =
        new Article(title, "desc", "body", Arrays.asList("java"), author.getId(), createdAt);
    articleRepository.save(a);
    return a;
  }

  private void loginAs(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  private ExecutionResult execute(String query, Map<String, Object> variables) {
    HttpHeaders headers = new HttpHeaders();
    MockHttpServletRequest request = new MockHttpServletRequest();
    headers.add("Authorization", "Token test-token");
    request.addHeader("Authorization", "Token test-token");
    return dgsQueryExecutor.execute(
        query, variables, null, headers, null, new ServletWebRequest(request));
  }

  private ExecutionResult execute(String query, String slug) {
    return execute(query, Collections.singletonMap("slug", slug));
  }

  private static <T> T read(ExecutionResult result, String path) {
    return JsonPath.read(result.toSpecification(), path);
  }

  private boolean isBookmarked(Article a, User user) {
    return articleBookmarkRepository.find(a.getId(), user.getId()).isPresent();
  }

  // AC-12
  @Test
  public void ac12_bookmarkArticle_returns_bookmarked_true_and_is_idempotent() {
    loginAs(reader);
    for (int i = 0; i < 2; i++) {
      ExecutionResult result = execute(BOOKMARK, article.getSlug());
      Assertions.assertTrue(result.getErrors().isEmpty(), result.getErrors().toString());
      Assertions.assertEquals(
          article.getSlug(), read(result, "$.data.bookmarkArticle.article.slug"));
      Assertions.assertEquals(true, read(result, "$.data.bookmarkArticle.article.bookmarked"));
      Assertions.assertEquals(false, read(result, "$.data.bookmarkArticle.article.favorited"));
    }
    Assertions.assertTrue(isBookmarked(article, reader));
    List<Object> edges = read(execute(MY_BOOKMARKS, new HashMap<>()), "$.data.me.bookmarks.edges");
    Assertions.assertEquals(1, edges.size());
  }

  // AC-13
  @Test
  public void ac13_unbookmarkArticle_returns_bookmarked_false_and_is_idempotent() {
    articleBookmarkRepository.save(new ArticleBookmark(article.getId(), reader.getId()));
    loginAs(reader);
    for (int i = 0; i < 2; i++) {
      ExecutionResult result = execute(UNBOOKMARK, article.getSlug());
      Assertions.assertTrue(result.getErrors().isEmpty(), result.getErrors().toString());
      Assertions.assertEquals(false, read(result, "$.data.unbookmarkArticle.article.bookmarked"));
    }
    Assertions.assertFalse(isBookmarked(article, reader));
  }

  // AC-14
  @Test
  public void ac14_bookmark_mutations_require_authentication() {
    anonymous();
    for (String mutation : Arrays.asList(BOOKMARK, UNBOOKMARK)) {
      ExecutionResult result = execute(mutation, article.getSlug());
      Assertions.assertFalse(result.getErrors().isEmpty());
    }
    Assertions.assertFalse(isBookmarked(article, reader));
  }

  // AC-14
  @Test
  public void ac14_bookmark_mutations_fail_for_unknown_slug() {
    loginAs(reader);
    for (String mutation : Arrays.asList(BOOKMARK, UNBOOKMARK)) {
      ExecutionResult result = execute(mutation, "no-such-article");
      Assertions.assertFalse(result.getErrors().isEmpty());
    }
  }

  // AC-9
  @Test
  public void ac9_article_bookmarked_field_reflects_viewer() {
    articleBookmarkRepository.save(new ArticleBookmark(article.getId(), reader.getId()));

    loginAs(reader);
    Assertions.assertEquals(
        true, read(execute(ARTICLE, article.getSlug()), "$.data.article.bookmarked"));
    loginAs(author);
    Assertions.assertEquals(
        false, read(execute(ARTICLE, article.getSlug()), "$.data.article.bookmarked"));
    anonymous();
    Assertions.assertEquals(
        false, read(execute(ARTICLE, article.getSlug()), "$.data.article.bookmarked"));
  }

  // AC-15
  @Test
  public void ac15_viewer_bookmarks_are_paged_newest_first_by_cursor() {
    Article second = article("second " + reader.getUsername(), new DateTime().minusHours(2));
    Article third = article("third " + reader.getUsername(), new DateTime().minusHours(3));
    DateTime base = new DateTime().minusMinutes(30).withMillisOfSecond(0);
    articleBookmarkRepository.save(
        new ArticleBookmark(article.getId(), reader.getId(), base.plusMinutes(1)));
    articleBookmarkRepository.save(
        new ArticleBookmark(second.getId(), reader.getId(), base.plusMinutes(2)));
    articleBookmarkRepository.save(
        new ArticleBookmark(third.getId(), reader.getId(), base.plusMinutes(3)));

    loginAs(reader);
    Map<String, Object> vars = new HashMap<>();
    vars.put("first", 2);
    ExecutionResult page1 = execute(MY_BOOKMARKS, vars);
    Assertions.assertTrue(page1.getErrors().isEmpty(), page1.getErrors().toString());
    Assertions.assertEquals(reader.getUsername(), read(page1, "$.data.me.username"));
    List<String> slugs1 = read(page1, "$.data.me.bookmarks.edges[*].node.slug");
    Assertions.assertEquals(Arrays.asList(third.getSlug(), second.getSlug()), slugs1);
    Assertions.assertEquals(
        author.getUsername(), read(page1, "$.data.me.bookmarks.edges[0].node.author.username"));
    Assertions.assertEquals(true, read(page1, "$.data.me.bookmarks.edges[0].node.bookmarked"));
    Assertions.assertEquals(true, read(page1, "$.data.me.bookmarks.pageInfo.hasNextPage"));
    String endCursor = read(page1, "$.data.me.bookmarks.pageInfo.endCursor");
    Assertions.assertEquals(
        String.valueOf(base.plusMinutes(2).getMillis()),
        endCursor,
        "cursor is bookmark time, not article time");

    vars.put("after", endCursor);
    ExecutionResult page2 = execute(MY_BOOKMARKS, vars);
    Assertions.assertTrue(page2.getErrors().isEmpty(), page2.getErrors().toString());
    List<String> slugs2 = read(page2, "$.data.me.bookmarks.edges[*].node.slug");
    Assertions.assertEquals(Arrays.asList(article.getSlug()), slugs2);
    Assertions.assertEquals(false, read(page2, "$.data.me.bookmarks.pageInfo.hasNextPage"));
  }

  // AC-16
  @Test
  public void ac16_deleted_articles_are_excluded_from_viewer_bookmarks() {
    Article doomed = article("doomed " + reader.getUsername(), new DateTime().minusHours(2));
    articleBookmarkRepository.save(new ArticleBookmark(article.getId(), reader.getId()));
    articleBookmarkRepository.save(new ArticleBookmark(doomed.getId(), reader.getId()));
    articleRepository.remove(doomed);

    loginAs(reader);
    ExecutionResult result = execute(MY_BOOKMARKS, new HashMap<>());
    Assertions.assertTrue(result.getErrors().isEmpty(), result.getErrors().toString());
    List<String> slugs = read(result, "$.data.me.bookmarks.edges[*].node.slug");
    Assertions.assertEquals(Arrays.asList(article.getSlug()), slugs);
  }
}
