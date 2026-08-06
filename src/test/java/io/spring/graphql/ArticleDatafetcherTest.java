package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {DgsAutoConfiguration.class, ArticleDatafetcher.class, ProfileDatafetcher.class})
public class ArticleDatafetcherTest extends DgsTestBase {

  private static final String ARTICLES_FRAGMENT =
      "edges { cursor node { slug title description body favorited favoritesCount tagList"
          + " createdAt updatedAt } } pageInfo { hasNextPage hasPreviousPage startCursor"
          + " endCursor }";

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;
  @MockBean private UserRepository userRepository;
  @MockBean private ProfileQueryService profileQueryService;

  private User user;
  private ArticleData articleData;

  @BeforeEach
  void setUp() {
    user = TestHelper.userFixture("author");
    articleData = TestHelper.articleDataFixture("one", user);
    authenticateAnonymously();
  }

  private CursorPager<ArticleData> pager(
      List<ArticleData> data, Direction direction, boolean more) {
    return new CursorPager<>(data, direction, more);
  }

  private Map<String, Object> edgeNode(Map<String, Object> connection, int index) {
    List<Map<String, Object>> edges = (List<Map<String, Object>>) connection.get("edges");
    return (Map<String, Object>) edges.get(index).get("node");
  }

  @Test
  public void should_return_feed_of_current_user_paging_forward() {
    authenticate(user);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pager(Arrays.asList(articleData), Direction.NEXT, true));

    Map<String, Object> connection =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ feed(first: 10) { " + ARTICLES_FRAGMENT + " } }", "data.feed");

    assertThat((List<?>) connection.get("edges")).hasSize(1);
    Map<String, Object> pageInfo = (Map<String, Object>) connection.get("pageInfo");
    assertThat(pageInfo.get("hasNextPage")).isEqualTo(true);
    assertThat(pageInfo.get("hasPreviousPage")).isEqualTo(false);
    assertThat(pageInfo.get("startCursor")).isEqualTo(articleData.getCursor().toString());
    assertThat(pageInfo.get("endCursor")).isEqualTo(articleData.getCursor().toString());

    Map<String, Object> node = edgeNode(connection, 0);
    assertThat(node.get("slug")).isEqualTo(articleData.getSlug());

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService).findUserFeedWithCursor(eq(user), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.NEXT);
    assertThat(captor.getValue().getLimit()).isEqualTo(10);
    assertThat(captor.getValue().getCursor()).isNull();
  }

  @Test
  public void should_page_feed_backward_with_last_and_before_cursor() {
    authenticate(user);
    DateTime cursorTime = new DateTime().minusDays(1);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pager(Arrays.asList(articleData), Direction.PREV, true));

    Map<String, Object> pageInfo =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ feed(last: 5, before: \""
                + cursorTime.getMillis()
                + "\") { "
                + ARTICLES_FRAGMENT
                + " } }",
            "data.feed.pageInfo");

    assertThat(pageInfo.get("hasNextPage")).isEqualTo(false);
    assertThat(pageInfo.get("hasPreviousPage")).isEqualTo(true);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService).findUserFeedWithCursor(eq(user), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getLimit()).isEqualTo(5);
    assertThat(captor.getValue().getCursor().getMillis()).isEqualTo(cursorTime.getMillis());
  }

  @Test
  public void should_return_empty_feed_with_null_cursors() {
    authenticate(user);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pager(Collections.emptyList(), Direction.NEXT, false));

    Map<String, Object> connection =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ feed(first: 10) { " + ARTICLES_FRAGMENT + " } }", "data.feed");

    assertThat((List<?>) connection.get("edges")).isEmpty();
    Map<String, Object> pageInfo = (Map<String, Object>) connection.get("pageInfo");
    assertThat(pageInfo.get("startCursor")).isNull();
    assertThat(pageInfo.get("endCursor")).isNull();
    assertThat(pageInfo.get("hasNextPage")).isEqualTo(false);
  }

  @Test
  public void should_fail_feed_when_neither_first_nor_last_is_given() {
    authenticate(user);

    ExecutionResult result = dgsQueryExecutor.execute("{ feed { pageInfo { hasNextPage } } }");

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_return_articles_filtered_by_tag_and_author() {
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq("author"), eq("fan"), any(), isNull()))
        .thenReturn(pager(Arrays.asList(articleData), Direction.NEXT, false));

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ articles(first: 10, withTag: \"java\", authoredBy: \"author\", favoritedBy:"
                + " \"fan\") { "
                + ARTICLES_FRAGMENT
                + " } }",
            "data.articles.edges[0].node.slug");

    assertThat(slug).isEqualTo(articleData.getSlug());
  }

  @Test
  public void should_page_articles_backward() {
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), isNull()))
        .thenReturn(pager(Arrays.asList(articleData), Direction.PREV, true));

    Map<String, Object> pageInfo =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ articles(last: 3) { " + ARTICLES_FRAGMENT + " } }", "data.articles.pageInfo");

    assertThat(pageInfo.get("hasPreviousPage")).isEqualTo(true);
    assertThat(pageInfo.get("hasNextPage")).isEqualTo(false);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), isNull(), isNull(), captor.capture(), isNull());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getLimit()).isEqualTo(3);
  }

  @Test
  public void should_fail_articles_when_neither_first_nor_last_is_given() {
    ExecutionResult result = dgsQueryExecutor.execute("{ articles { pageInfo { hasNextPage } } }");

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_find_article_by_slug_with_author() {
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), isNull()))
        .thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(user.getUsername()), isNull()))
        .thenReturn(Optional.of(TestHelper.profileDataFixture(user)));

    Map<String, Object> article =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ article(slug: \""
                + articleData.getSlug()
                + "\") { slug title body favorited favoritesCount author { username bio image"
                + " following } } }",
            "data.article");

    assertThat(article.get("title")).isEqualTo(articleData.getTitle());
    assertThat(((Map<String, Object>) article.get("author")).get("username"))
        .isEqualTo(user.getUsername());
  }

  @Test
  public void should_fail_finding_article_by_unknown_slug() {
    when(articleQueryService.findBySlug(eq("unknown"), isNull())).thenReturn(Optional.empty());

    ExecutionResult result = dgsQueryExecutor.execute("{ article(slug: \"unknown\") { slug } }");

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_return_articles_of_a_profile() {
    when(profileQueryService.findByUsername(eq(user.getUsername()), isNull()))
        .thenReturn(Optional.of(TestHelper.profileDataFixture(user)));
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq(user.getUsername()), isNull(), any(), isNull()))
        .thenReturn(pager(Arrays.asList(articleData), Direction.NEXT, false));

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ profile(username: \""
                + user.getUsername()
                + "\") { profile { username articles(first: 10) { edges { node { slug } } } } } }",
            "data.profile.profile.articles.edges[0].node.slug");

    assertThat(slug).isEqualTo(articleData.getSlug());
  }

  @Test
  public void should_return_favorites_of_a_profile_paging_backward() {
    when(profileQueryService.findByUsername(eq(user.getUsername()), isNull()))
        .thenReturn(Optional.of(TestHelper.profileDataFixture(user)));
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq(user.getUsername()), any(), isNull()))
        .thenReturn(pager(Arrays.asList(articleData), Direction.PREV, true));

    Map<String, Object> pageInfo =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ profile(username: \""
                + user.getUsername()
                + "\") { profile { favorites(last: 2) { edges { node { slug } } pageInfo {"
                + " hasPreviousPage } } } } }",
            "data.profile.profile.favorites.pageInfo");

    assertThat(pageInfo.get("hasPreviousPage")).isEqualTo(true);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(
            isNull(), isNull(), eq(user.getUsername()), captor.capture(), isNull());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getLimit()).isEqualTo(2);
  }

  @Test
  public void should_return_feed_of_a_profile() {
    when(profileQueryService.findByUsername(eq(user.getUsername()), isNull()))
        .thenReturn(Optional.of(TestHelper.profileDataFixture(user)));
    when(userRepository.findByUsername(eq(user.getUsername()))).thenReturn(Optional.of(user));
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pager(Arrays.asList(articleData), Direction.NEXT, false));

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ profile(username: \""
                + user.getUsername()
                + "\") { profile { feed(first: 10) { edges { node { slug } } } } } }",
            "data.profile.profile.feed.edges[0].node.slug");

    assertThat(slug).isEqualTo(articleData.getSlug());
  }

  @Test
  public void should_fail_profile_feed_when_user_does_not_exist_anymore() {
    when(profileQueryService.findByUsername(eq(user.getUsername()), isNull()))
        .thenReturn(Optional.of(TestHelper.profileDataFixture(user)));
    when(userRepository.findByUsername(eq(user.getUsername()))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ profile(username: \""
                + user.getUsername()
                + "\") { profile { feed(first: 10) { edges { node { slug } } } } } }");

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_fail_profile_feed_when_neither_first_nor_last_is_given() {
    when(profileQueryService.findByUsername(eq(user.getUsername()), isNull()))
        .thenReturn(Optional.of(TestHelper.profileDataFixture(user)));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ profile(username: \""
                + user.getUsername()
                + "\") { profile { feed { edges { node { slug } } } } } }");

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_fail_profile_articles_when_neither_first_nor_last_is_given() {
    when(profileQueryService.findByUsername(eq(user.getUsername()), isNull()))
        .thenReturn(Optional.of(TestHelper.profileDataFixture(user)));

    ExecutionResult articlesResult =
        dgsQueryExecutor.execute(
            "{ profile(username: \""
                + user.getUsername()
                + "\") { profile { articles { edges { node { slug } } } } } }");
    assertThat(articlesResult.getErrors()).isNotEmpty();

    ExecutionResult favoritesResult =
        dgsQueryExecutor.execute(
            "{ profile(username: \""
                + user.getUsername()
                + "\") { profile { favorites { edges { node { slug } } } } } }");
    assertThat(favoritesResult.getErrors()).isNotEmpty();
  }
}
