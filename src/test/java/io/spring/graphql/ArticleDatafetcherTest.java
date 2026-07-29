package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.TestHelper;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.jupiter.api.AfterEach;
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
    classes = {DgsAutoConfiguration.class, ArticleDatafetcher.class, ProfileDatafetcher.class})
public class ArticleDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @Autowired private ArticleDatafetcher articleDatafetcher;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private UserRepository userRepository;

  @MockBean private ProfileQueryService profileQueryService;

  private User currentUser;
  private User author;
  private ArticleData articleData;

  @BeforeEach
  public void setUp() {
    currentUser = new User("current@test.com", "current", "123", "", "");
    author = new User("author@test.com", "author", "123", "author bio", "author image");
    articleData = TestHelper.articleDataFixture("1", author);
    authenticate(currentUser);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_return_feed_of_the_current_user_paging_forwards() {
    ArticleData second = TestHelper.articleDataFixture("2", author);
    when(articleQueryService.findUserFeedWithCursor(eq(currentUser), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(articleData, second), Direction.NEXT, true));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ feed(first: 2, after: \"1000\") { edges { cursor node { slug title body favorited"
                + " favoritesCount createdAt } } pageInfo { hasNextPage hasPreviousPage startCursor"
                + " endCursor } } }");

    assertThat(result.<List<String>>read("data.feed.edges[*].node.slug"))
        .containsExactly(articleData.getSlug(), second.getSlug());
    assertThat(result.<String>read("data.feed.edges[0].cursor"))
        .isEqualTo(articleData.getCursor().toString());
    assertThat(result.<String>read("data.feed.edges[0].node.createdAt"))
        .isEqualTo(ISODateTimeFormat.dateTime().withZoneUTC().print(articleData.getCreatedAt()));
    assertThat(result.<Boolean>read("data.feed.pageInfo.hasNextPage")).isTrue();
    assertThat(result.<Boolean>read("data.feed.pageInfo.hasPreviousPage")).isFalse();
    assertThat(result.<String>read("data.feed.pageInfo.startCursor"))
        .isEqualTo(articleData.getCursor().toString());
    assertThat(result.<String>read("data.feed.pageInfo.endCursor"))
        .isEqualTo(second.getCursor().toString());

    CursorPageParameter<DateTime> page = captureFeedPageParameter();
    assertThat(page.getDirection()).isEqualTo(Direction.NEXT);
    assertThat(page.getLimit()).isEqualTo(2);
    assertThat(page.getCursor().getMillis()).isEqualTo(1000L);
  }

  @Test
  public void should_return_feed_paging_backwards() {
    when(articleQueryService.findUserFeedWithCursor(eq(currentUser), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(articleData), Direction.PREV, true));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ feed(last: 1, before: \"2000\") { edges { node { slug } } pageInfo { hasNextPage"
                + " hasPreviousPage } } }");

    assertThat(result.<Boolean>read("data.feed.pageInfo.hasPreviousPage")).isTrue();
    assertThat(result.<Boolean>read("data.feed.pageInfo.hasNextPage")).isFalse();

    CursorPageParameter<DateTime> page = captureFeedPageParameter();
    assertThat(page.getDirection()).isEqualTo(Direction.PREV);
    assertThat(page.getLimit()).isEqualTo(1);
    assertThat(page.getCursor().getMillis()).isEqualTo(2000L);
  }

  @Test
  public void should_return_null_cursors_for_an_empty_feed() {
    when(articleQueryService.findUserFeedWithCursor(eq(currentUser), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ feed(first: 10) { edges { cursor } pageInfo { startCursor endCursor hasNextPage } }"
                + " }");

    assertThat(result.<List<Object>>read("data.feed.edges")).isEmpty();
    assertThat(result.<String>read("data.feed.pageInfo.startCursor")).isNull();
    assertThat(result.<String>read("data.feed.pageInfo.endCursor")).isNull();
    assertThat(result.<Boolean>read("data.feed.pageInfo.hasNextPage")).isFalse();
  }

  @Test
  public void should_query_feed_without_current_user_when_anonymous() {
    anonymous();
    when(articleQueryService.findUserFeedWithCursor(isNull(), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(articleData), Direction.NEXT, false));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ feed(first: 1) { edges { node { slug } } } }");

    assertThat(result.<String>read("data.feed.edges[0].node.slug"))
        .isEqualTo(articleData.getSlug());
  }

  @Test
  public void should_report_error_when_feed_has_neither_first_nor_last() {
    ExecutionResult result = dgsQueryExecutor.execute("{ feed { edges { cursor } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("IllegalArgumentException");
  }

  @Test
  public void should_return_articles_filtered_by_tag_author_and_favorited_by() {
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq("author"), eq("current"), any(), eq(currentUser)))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(articleData), Direction.NEXT, false));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ articles(first: 1, withTag: \"java\", authoredBy: \"author\", favoritedBy:"
                + " \"current\") { edges { node { slug title } } pageInfo { hasNextPage } } }");

    assertThat(result.<String>read("data.articles.edges[0].node.slug"))
        .isEqualTo(articleData.getSlug());
    assertThat(result.<String>read("data.articles.edges[0].node.title"))
        .isEqualTo(articleData.getTitle());
    assertThat(result.<Boolean>read("data.articles.pageInfo.hasNextPage")).isFalse();
  }

  @Test
  public void should_return_articles_paging_backwards() {
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), eq(currentUser)))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(articleData), Direction.PREV, true));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ articles(last: 5, before: \"3000\") { edges { node { slug } } pageInfo {"
                + " hasPreviousPage } } }");

    assertThat(result.<Boolean>read("data.articles.pageInfo.hasPreviousPage")).isTrue();

    CursorPageParameter<DateTime> page = captureRecentArticlesPageParameter();
    assertThat(page.getDirection()).isEqualTo(Direction.PREV);
    assertThat(page.getLimit()).isEqualTo(5);
    assertThat(page.getCursor().getMillis()).isEqualTo(3000L);
  }

  @Test
  public void should_report_error_when_articles_has_neither_first_nor_last() {
    ExecutionResult result = dgsQueryExecutor.execute("{ articles { edges { cursor } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("IllegalArgumentException");
  }

  @Test
  public void should_find_article_by_slug() {
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), eq(currentUser)))
        .thenReturn(Optional.of(articleData));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "{ article(slug: \"%s\") { slug title description body favorited favoritesCount"
                    + " tagList createdAt updatedAt } }",
                articleData.getSlug()));

    assertThat(result.<String>read("data.article.title")).isEqualTo(articleData.getTitle());
    assertThat(result.<String>read("data.article.description"))
        .isEqualTo(articleData.getDescription());
    assertThat(result.<String>read("data.article.body")).isEqualTo(articleData.getBody());
    assertThat(result.<Boolean>read("data.article.favorited")).isFalse();
    assertThat(result.<Integer>read("data.article.favoritesCount")).isZero();
    assertThat(result.<String>read("data.article.updatedAt"))
        .isEqualTo(ISODateTimeFormat.dateTime().withZoneUTC().print(articleData.getUpdatedAt()));
  }

  @Test
  public void should_report_error_when_article_is_not_found() {
    when(articleQueryService.findBySlug(eq("missing"), any())).thenReturn(Optional.empty());

    ExecutionResult result = dgsQueryExecutor.execute("{ article(slug: \"missing\") { slug } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  @Test
  public void should_return_the_feed_of_a_profile() {
    mockProfile();
    when(userRepository.findByUsername(eq(author.getUsername()))).thenReturn(Optional.of(author));
    when(articleQueryService.findUserFeedWithCursor(eq(author), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(articleData), Direction.NEXT, false));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "{ profile(username: \"%s\") { profile { username feed(first: 1) { edges { cursor"
                    + " node { slug } } } } } }",
                author.getUsername()));

    assertThat(result.<String>read("data.profile.profile.username"))
        .isEqualTo(author.getUsername());
    assertThat(result.<String>read("data.profile.profile.feed.edges[0].node.slug"))
        .isEqualTo(articleData.getSlug());
  }

  @Test
  public void should_report_error_when_the_profile_of_a_feed_has_no_user() {
    mockProfile();
    when(userRepository.findByUsername(eq(author.getUsername()))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "{ profile(username: \"%s\") { profile { feed(first: 1) { edges { cursor } } } } }",
                author.getUsername()));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  @Test
  public void should_return_the_articles_authored_by_a_profile() {
    mockProfile();
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq(author.getUsername()), isNull(), any(), eq(currentUser)))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(articleData), Direction.NEXT, true));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "{ profile(username: \"%s\") { profile { articles(first: 1) { edges { node { slug"
                    + " } } pageInfo { hasNextPage } } } } }",
                author.getUsername()));

    assertThat(result.<String>read("data.profile.profile.articles.edges[0].node.slug"))
        .isEqualTo(articleData.getSlug());
    assertThat(result.<Boolean>read("data.profile.profile.articles.pageInfo.hasNextPage")).isTrue();
  }

  @Test
  public void should_return_the_articles_favorited_by_a_profile_paging_backwards() {
    mockProfile();
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq(author.getUsername()), any(), eq(currentUser)))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(articleData), Direction.PREV, true));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "{ profile(username: \"%s\") { profile { favorites(last: 3, before: \"4000\") {"
                    + " edges { node { slug } } pageInfo { hasPreviousPage } } } } }",
                author.getUsername()));

    assertThat(result.<String>read("data.profile.profile.favorites.edges[0].node.slug"))
        .isEqualTo(articleData.getSlug());
    assertThat(result.<Boolean>read("data.profile.profile.favorites.pageInfo.hasPreviousPage"))
        .isTrue();

    CursorPageParameter<DateTime> page = captureRecentArticlesPageParameter();
    assertThat(page.getDirection()).isEqualTo(Direction.PREV);
    assertThat(page.getLimit()).isEqualTo(3);
    assertThat(page.getCursor().getMillis()).isEqualTo(4000L);
  }

  @Test
  public void should_report_error_when_profile_articles_have_neither_first_nor_last() {
    mockProfile();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "{ profile(username: \"%s\") { profile { articles { edges { cursor } } } } }",
                author.getUsername()));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("IllegalArgumentException");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void should_resolve_the_article_of_a_comment_from_its_local_context() {
    when(articleQueryService.findById(eq(articleData.getId()), eq(currentUser)))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<io.spring.graphql.types.Article> result =
        articleDatafetcher.getCommentArticle(commentEnvironment());

    assertThat(result.getData().getSlug()).isEqualTo(articleData.getSlug());
    assertThat(result.getData().getTitle()).isEqualTo(articleData.getTitle());
    Map<String, Object> localContext = (Map<String, Object>) result.getLocalContext();
    assertThat(localContext).containsEntry(articleData.getSlug(), articleData);
  }

  @Test
  public void should_fail_to_resolve_the_article_of_a_comment_when_it_is_gone() {
    when(articleQueryService.findById(eq(articleData.getId()), eq(currentUser)))
        .thenReturn(Optional.empty());

    DataFetchingEnvironment environment = commentEnvironment();

    assertThatThrownBy(() -> articleDatafetcher.getCommentArticle(environment))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  private DataFetchingEnvironment commentEnvironment() {
    DateTime now = new DateTime();
    CommentData commentData =
        new CommentData("comment-1", "a comment", articleData.getId(), now, now, null);
    DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
    when(environment.<CommentData>getLocalContext()).thenReturn(commentData);
    return environment;
  }

  private void mockProfile() {
    when(profileQueryService.findByUsername(eq(author.getUsername()), eq(currentUser)))
        .thenReturn(
            Optional.of(
                new ProfileData(
                    author.getId(),
                    author.getUsername(),
                    author.getBio(),
                    author.getImage(),
                    true)));
  }

  @SuppressWarnings("unchecked")
  private CursorPageParameter<DateTime> captureFeedPageParameter() {
    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService).findUserFeedWithCursor(any(), captor.capture());
    return captor.getValue();
  }

  @SuppressWarnings("unchecked")
  private CursorPageParameter<DateTime> captureRecentArticlesPageParameter() {
    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(any(), any(), any(), captor.capture(), any());
    return captor.getValue();
  }

  private void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }
}
