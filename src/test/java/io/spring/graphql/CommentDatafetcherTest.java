package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
      ProfileDatafetcher.class
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

    List<String> bodies =
        dgsQueryExecutor.executeAndExtractJsonPath(
            query, "data.article.comments.edges[*].node.body");
    List<String> ids =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.comments.edges[*].node.id");
    List<String> authors =
        dgsQueryExecutor.executeAndExtractJsonPath(
            query, "data.article.comments.edges[*].node.author.username");
    Boolean hasNext =
        dgsQueryExecutor.executeAndExtractJsonPath(
            query, "data.article.comments.pageInfo.hasNextPage");

    assertThat(bodies).containsExactly("first comment", "second comment");
    assertThat(ids).containsExactly("c1", "c2");
    assertThat(authors).containsExactly("author", "author");
    assertThat(hasNext).isFalse();
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
    verify(commentQueryService, atLeastOnce())
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

    assertThat(result.getErrors()).isNotEmpty();
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

    List<Object> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.comments.edges");
    String startCursor =
        dgsQueryExecutor.executeAndExtractJsonPath(
            query, "data.article.comments.pageInfo.startCursor");

    assertThat(edges).isEmpty();
    assertThat(startCursor).isNull();
  }
}
