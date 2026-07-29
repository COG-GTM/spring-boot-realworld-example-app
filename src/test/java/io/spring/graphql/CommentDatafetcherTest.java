package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    classes = {DgsAutoConfiguration.class, CommentDatafetcher.class, ArticleDatafetcher.class})
public class CommentDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private CommentQueryService commentQueryService;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private UserRepository userRepository;

  private User currentUser;
  private User author;
  private ArticleData articleData;
  private CommentData commentData;

  @BeforeEach
  public void setUp() {
    currentUser = new User("current@test.com", "current", "123", "", "");
    author = new User("author@test.com", "author", "123", "author bio", "author image");
    articleData = TestHelper.articleDataFixture("1", author);
    commentData = commentDataFixture("comment-1", "first comment");
    authenticate(currentUser);
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), any()))
        .thenReturn(Optional.of(articleData));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_return_article_comments_paging_forwards() {
    CommentData second = commentDataFixture("comment-2", "second comment");
    when(commentQueryService.findByArticleIdWithCursor(
            eq(articleData.getId()), eq(currentUser), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(commentData, second), Direction.NEXT, true));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(commentsQuery("first: 2, after: \"1000\""));

    assertThat(result.<List<String>>read("data.article.comments.edges[*].node.id"))
        .containsExactly(commentData.getId(), second.getId());
    assertThat(result.<String>read("data.article.comments.edges[0].node.body"))
        .isEqualTo(commentData.getBody());
    assertThat(result.<String>read("data.article.comments.edges[0].cursor"))
        .isEqualTo(commentData.getCursor().toString());
    assertThat(result.<String>read("data.article.comments.edges[0].node.createdAt"))
        .isEqualTo(ISODateTimeFormat.dateTime().withZoneUTC().print(commentData.getCreatedAt()));
    // the datafetcher maps updatedAt from createdAt, so it never reflects the real updatedAt
    assertThat(result.<String>read("data.article.comments.edges[0].node.updatedAt"))
        .isEqualTo(ISODateTimeFormat.dateTime().withZoneUTC().print(commentData.getCreatedAt()));
    assertThat(result.<Boolean>read("data.article.comments.pageInfo.hasNextPage")).isTrue();
    assertThat(result.<Boolean>read("data.article.comments.pageInfo.hasPreviousPage")).isFalse();
    assertThat(result.<String>read("data.article.comments.pageInfo.startCursor"))
        .isEqualTo(commentData.getCursor().toString());
    assertThat(result.<String>read("data.article.comments.pageInfo.endCursor"))
        .isEqualTo(second.getCursor().toString());

    CursorPageParameter<DateTime> page = capturePageParameter();
    assertThat(page.getDirection()).isEqualTo(Direction.NEXT);
    assertThat(page.getLimit()).isEqualTo(2);
    assertThat(page.getCursor().getMillis()).isEqualTo(1000L);
  }

  @Test
  public void should_return_article_comments_paging_backwards() {
    when(commentQueryService.findByArticleIdWithCursor(
            eq(articleData.getId()), eq(currentUser), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(commentData), Direction.PREV, true));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(commentsQuery("last: 1, before: \"2000\""));

    assertThat(result.<Boolean>read("data.article.comments.pageInfo.hasPreviousPage")).isTrue();
    assertThat(result.<Boolean>read("data.article.comments.pageInfo.hasNextPage")).isFalse();

    CursorPageParameter<DateTime> page = capturePageParameter();
    assertThat(page.getDirection()).isEqualTo(Direction.PREV);
    assertThat(page.getLimit()).isEqualTo(1);
    assertThat(page.getCursor().getMillis()).isEqualTo(2000L);
  }

  @Test
  public void should_return_empty_comments_connection() {
    when(commentQueryService.findByArticleIdWithCursor(
            eq(articleData.getId()), eq(currentUser), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(commentsQuery("first: 10"));

    assertThat(result.<List<Object>>read("data.article.comments.edges")).isEmpty();
    assertThat(result.<String>read("data.article.comments.pageInfo.startCursor")).isNull();
    assertThat(result.<String>read("data.article.comments.pageInfo.endCursor")).isNull();
  }

  @Test
  public void should_load_comments_without_current_user_when_anonymous() {
    anonymous();
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), isNull(), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(commentData), Direction.NEXT, false));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(commentsQuery("first: 1"));

    assertThat(result.<String>read("data.article.comments.edges[0].node.id"))
        .isEqualTo(commentData.getId());
  }

  @Test
  public void should_report_error_when_comments_have_neither_first_nor_last() {
    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "{ article(slug: \"%s\") { comments { edges { cursor } } } }",
                articleData.getSlug()));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("IllegalArgumentException");
  }

  private String commentsQuery(String pagingArguments) {
    return String.format(
        "{ article(slug: \"%s\") { slug comments(%s) { edges { cursor node { id body createdAt"
            + " updatedAt } } pageInfo { hasNextPage hasPreviousPage startCursor endCursor } } } }",
        articleData.getSlug(), pagingArguments);
  }

  @SuppressWarnings("unchecked")
  private CursorPageParameter<DateTime> capturePageParameter() {
    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(commentQueryService).findByArticleIdWithCursor(any(), any(), captor.capture());
    return captor.getValue();
  }

  private CommentData commentDataFixture(String id, String body) {
    DateTime createdAt = new DateTime();
    return new CommentData(
        id,
        body,
        articleData.getId(),
        createdAt,
        createdAt.plusDays(1),
        new ProfileData(
            author.getId(), author.getUsername(), author.getBio(), author.getImage(), false));
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
