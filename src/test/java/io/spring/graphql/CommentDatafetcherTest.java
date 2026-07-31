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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ArticleDatafetcher.class,
      CommentDatafetcher.class,
      ProfileDatafetcher.class
    })
class CommentDatafetcherTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;
  @MockBean private CommentQueryService commentQueryService;
  @MockBean private ProfileQueryService profileQueryService;
  @MockBean private UserRepository userRepository;

  private User author;
  private ArticleData articleData;
  private CommentData commentData;

  @BeforeEach
  void setUp() {
    author = userFixture("john");
    articleData = TestHelper.articleDataFixture("test", author);
    commentData = commentDataFixture("1", articleData.getId(), author);
    anonymous();
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), isNull()))
        .thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(author.getUsername()), any()))
        .thenReturn(
            Optional.of(
                new ProfileData(
                    author.getId(),
                    author.getUsername(),
                    author.getBio(),
                    author.getImage(),
                    false)));
  }

  @Test
  void should_query_comments_of_an_article_with_author() {
    CommentData another = commentDataFixture("2", articleData.getId(), author);
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), isNull(), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(commentData, another), Direction.NEXT, true));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "{ article(slug: \"%s\") { comments(first: 2) { edges { cursor node { id body author { username } } } pageInfo { hasNextPage } } } }",
                articleData.getSlug()));

    assertThat(context.<List<String>>read("data.article.comments.edges[*].node.id"))
        .containsExactly("1", "2");
    assertThat(context.<List<String>>read("data.article.comments.edges[*].node.author.username"))
        .containsExactly(author.getUsername(), author.getUsername());
    assertThat(context.read("data.article.comments.pageInfo.hasNextPage", Boolean.class)).isTrue();
  }

  @Test
  void should_query_comments_backward_with_last_and_before_cursor() {
    DateTime cursorTime = commentData.getCreatedAt();
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), isNull(), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(commentData), Direction.PREV, true));

    Boolean hasPreviousPage =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "{ article(slug: \"%s\") { comments(last: 1, before: \"%d\") { pageInfo { hasPreviousPage hasNextPage } } } }",
                articleData.getSlug(), cursorTime.getMillis()),
            "data.article.comments.pageInfo.hasPreviousPage");

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(commentQueryService)
        .findByArticleIdWithCursor(eq(articleData.getId()), isNull(), captor.capture());
    assertThat(hasPreviousPage).isTrue();
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getCursor().getMillis()).isEqualTo(cursorTime.getMillis());
  }

  @Test
  void should_return_error_when_neither_first_nor_last_is_given() {
    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "{ article(slug: \"%s\") { comments { edges { cursor } } } }",
                articleData.getSlug()));

    assertFailedWith(result, IllegalArgumentException.class);
  }

  private CommentData commentDataFixture(String id, String articleId, User user) {
    DateTime now = new DateTime();
    return new CommentData(
        id,
        "comment body " + id,
        articleId,
        now,
        now,
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
  }
}
