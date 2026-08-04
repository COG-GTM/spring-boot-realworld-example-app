package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      CommentDatafetcher.class,
      ArticleDatafetcher.class,
      ProfileDatafetcher.class,
      GraphQLCustomizeExceptionHandler.class,
      RecordingExceptionHandler.class
    })
class CommentDatafetcherTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private CommentQueryService commentQueryService;
  // Collaborators of the datafetchers imported to reach Article.comments.
  @MockBean private ArticleQueryService articleQueryService;
  @MockBean private UserRepository userRepository;
  @MockBean private ProfileQueryService profileQueryService;

  private final User author = new User("a@example.com", "author", "123", "bio", "a.png");

  private CommentData commentData(String id, String body) {
    DateTime createdAt = new DateTime(1_600_000_000_000L);
    return new CommentData(
        id,
        body,
        "article-id",
        createdAt,
        createdAt,
        new ProfileData(author.getId(), "author", "bio", "a.png", false));
  }

  private ArticleData stubArticle() {
    ArticleData data = TestHelper.articleDataFixture("1", author);
    when(articleQueryService.findBySlug(eq(data.getSlug()), any())).thenReturn(Optional.of(data));
    when(profileQueryService.findByUsername(eq("author"), any()))
        .thenReturn(Optional.of(new ProfileData(author.getId(), "author", "bio", "a.png", false)));
    return data;
  }

  @Test
  void should_return_comments_of_an_article_with_author() {
    anonymous();
    ArticleData article = stubArticle();
    CommentData first = commentData("c1", "first comment");
    CommentData second = commentData("c2", "second comment");
    when(commentQueryService.findByArticleIdWithCursor(eq(article.getId()), eq(null), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(first, second), Direction.NEXT, false));

    String query =
        "{ article(slug: \""
            + article.getSlug()
            + "\") { comments(first: 10) { edges { cursor node { id body author { username } } }"
            + " pageInfo { hasNextPage } } } }";

    DocumentContext context = dgsQueryExecutor.executeAndGetDocumentContext(query);

    assertThat(context.<List<String>>read("data.article.comments.edges[*].node.body"))
        .containsExactly("first comment", "second comment");
    assertThat(context.<List<String>>read("data.article.comments.edges[*].node.id"))
        .containsExactly("c1", "c2");
    assertThat(context.<List<String>>read("data.article.comments.edges[*].node.author.username"))
        .containsExactly("author", "author");
    assertThat(context.read("data.article.comments.pageInfo.hasNextPage", Boolean.class)).isFalse();
    verify(commentQueryService).findByArticleIdWithCursor(eq(article.getId()), eq(null), any());
  }

  @Test
  void should_pass_current_user_and_prev_direction_when_paging_backwards() {
    authenticate(author);
    ArticleData article = stubArticle();
    CommentData comment = commentData("c1", "a comment");
    when(commentQueryService.findByArticleIdWithCursor(eq(article.getId()), eq(author), any()))
        .thenReturn(new CursorPager<>(Collections.singletonList(comment), Direction.PREV, true));

    String query =
        "{ article(slug: \""
            + article.getSlug()
            + "\") { comments(last: 3, before: \""
            + comment.getCursor()
            + "\") { edges { node { id } } pageInfo { hasPreviousPage } } } }";

    Boolean hasPrevious =
        dgsQueryExecutor.executeAndExtractJsonPath(
            query, "data.article.comments.pageInfo.hasPreviousPage");

    assertThat(hasPrevious).isTrue();
    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(commentQueryService)
        .findByArticleIdWithCursor(eq(article.getId()), eq(author), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getLimit()).isEqualTo(3);
    assertThat(captor.getValue().getCursor().getMillis())
        .isEqualTo(comment.getCreatedAt().getMillis());
  }

  @Test
  void should_error_when_neither_first_nor_last_given() {
    anonymous();
    ArticleData article = stubArticle();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ article(slug: \""
                + article.getSlug()
                + "\") { comments { edges { node { id } } } } }");

    assertSingleErrorFrom(result, IllegalArgumentException.class);
  }

  @Test
  void should_return_empty_connection_when_article_has_no_comments() {
    anonymous();
    ArticleData article = stubArticle();
    when(commentQueryService.findByArticleIdWithCursor(eq(article.getId()), eq(null), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    String query =
        "{ article(slug: \""
            + article.getSlug()
            + "\") { comments(first: 10) { edges { node { id } }"
            + " pageInfo { startCursor endCursor } } } }";

    DocumentContext context = dgsQueryExecutor.executeAndGetDocumentContext(query);

    assertThat(context.<List<Object>>read("data.article.comments.edges")).isEmpty();
    assertThat(context.read("data.article.comments.pageInfo.startCursor", String.class)).isNull();
    assertThat(context.read("data.article.comments.pageInfo.endCursor", String.class)).isNull();
  }
}
