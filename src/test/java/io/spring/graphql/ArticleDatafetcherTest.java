package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ArticleDatafetcher.class,
      ProfileDatafetcher.class,
      GraphQLCustomizeExceptionHandler.class
    })
public class ArticleDatafetcherTest {
  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private UserRepository userRepository;

  @MockBean private ProfileQueryService profileQueryService;

  private ArticleData first;
  private ArticleData second;

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    ProfileData author = new ProfileData("authorId", "jake", "bio", "image", false);
    first = articleData("first-article", new DateTime(2021, 1, 2, 0, 0, DateTimeZone.UTC), author);
    second =
        articleData("second-article", new DateTime(2021, 1, 1, 0, 0, DateTimeZone.UTC), author);
    when(profileQueryService.findByUsername(eq("jake"), any())).thenReturn(Optional.of(author));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private ArticleData articleData(String slug, DateTime time, ProfileData author) {
    return new ArticleData(
        slug + "-id",
        slug,
        slug,
        "desc",
        "body",
        false,
        0,
        time,
        time,
        Collections.singletonList("java"),
        author);
  }

  @Test
  public void should_query_articles_with_relay_page_info() {
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), isNull(), isNull(), any(), isNull()))
        .thenReturn(
            new CursorPager<>(new ArrayList<>(Arrays.asList(first, second)), Direction.NEXT, true));

    String query =
        "{ articles(first: 2, withTag: \"java\") { "
            + "edges { cursor node { slug createdAt author { username } } } "
            + "pageInfo { hasNextPage hasPreviousPage startCursor endCursor } } }";
    ExecutionResult result = dgsQueryExecutor.execute(query);

    Assertions.assertTrue(result.getErrors().isEmpty(), result.getErrors().toString());
    String startCursor = String.valueOf(first.getUpdatedAt().getMillis());
    String endCursor = String.valueOf(second.getUpdatedAt().getMillis());
    Assertions.assertEquals(
        true,
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasNextPage"));
    Assertions.assertEquals(
        false,
        dgsQueryExecutor.executeAndExtractJsonPath(
            query, "data.articles.pageInfo.hasPreviousPage"));
    Assertions.assertEquals(
        startCursor,
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.startCursor"));
    Assertions.assertEquals(
        endCursor,
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.endCursor"));
    List<String> slugs =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges[*].node.slug");
    Assertions.assertEquals(Arrays.asList("first-article", "second-article"), slugs);
    Assertions.assertEquals(
        startCursor,
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges[0].cursor"));
    Assertions.assertEquals(
        "2021-01-02T00:00:00.000Z",
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges[0].node.createdAt"));
    Assertions.assertEquals(
        "jake",
        dgsQueryExecutor.executeAndExtractJsonPath(
            query, "data.articles.edges[0].node.author.username"));
  }

  @Test
  public void should_return_null_cursors_for_empty_page() {
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(new CursorPager<>(new ArrayList<>(), Direction.NEXT, false));

    String query =
        "{ articles(first: 10) { edges { cursor } "
            + "pageInfo { hasNextPage hasPreviousPage startCursor endCursor } } }";
    ExecutionResult result = dgsQueryExecutor.execute(query);

    Assertions.assertTrue(result.getErrors().isEmpty(), result.getErrors().toString());
    Assertions.assertNull(
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.startCursor"));
    Assertions.assertNull(
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.endCursor"));
    Assertions.assertEquals(
        false,
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasNextPage"));
    List<Object> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");
    Assertions.assertTrue(edges.isEmpty());
  }

  @Test
  public void should_page_backward_with_last_and_before() {
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(
            new CursorPager<>(
                new ArrayList<>(Collections.singletonList(second)), Direction.PREV, true));

    long before = first.getUpdatedAt().getMillis();
    String query =
        "{ articles(last: 1, before: \""
            + before
            + "\") { pageInfo { hasNextPage hasPreviousPage } } }";

    Assertions.assertEquals(
        true,
        dgsQueryExecutor.executeAndExtractJsonPath(
            query, "data.articles.pageInfo.hasPreviousPage"));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), isNull(), isNull(), captor.capture(), isNull());
    CursorPageParameter<DateTime> page = captor.getValue();
    Assertions.assertEquals(Direction.PREV, page.getDirection());
    Assertions.assertEquals(1, page.getLimit());
    Assertions.assertEquals(before, page.getCursor().getMillis());
  }

  @Test
  public void should_return_error_when_neither_first_nor_last_given() {
    ExecutionResult result = dgsQueryExecutor.execute("{ articles { pageInfo { hasNextPage } } }");

    Assertions.assertEquals(1, result.getErrors().size());
    Assertions.assertEquals(
        Collections.singletonList("articles"), result.getErrors().get(0).getPath());
  }

  @Test
  public void should_query_feed_for_current_user() {
    User user = new User("john@jacob.com", "johnjacob", "123", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(
            new CursorPager<>(
                new ArrayList<>(Collections.singletonList(first)), Direction.NEXT, false));

    List<String> slugs =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ feed(first: 5) { edges { node { slug } } } }", "data.feed.edges[*].node.slug");

    Assertions.assertEquals(Collections.singletonList("first-article"), slugs);
    verify(articleQueryService).findUserFeedWithCursor(eq(user), any());
  }
}
