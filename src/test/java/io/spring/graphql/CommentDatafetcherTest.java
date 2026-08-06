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
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
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
    classes = {
      DgsAutoConfiguration.class,
      ArticleDatafetcher.class,
      CommentDatafetcher.class,
      ProfileDatafetcher.class
    })
public class CommentDatafetcherTest extends DgsTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;
  @MockBean private CommentQueryService commentQueryService;
  @MockBean private ProfileQueryService profileQueryService;
  @MockBean private UserRepository userRepository;

  private User user;
  private ArticleData articleData;
  private CommentData commentData;

  @BeforeEach
  void setUp() {
    user = TestHelper.userFixture("commenter");
    articleData = TestHelper.articleDataFixture("commented", user);
    commentData = TestHelper.commentDataFixture("first", user);
    authenticateAnonymously();
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), isNull()))
        .thenReturn(Optional.of(articleData));
  }

  private String commentsQuery(String args) {
    return "{ article(slug: \""
        + articleData.getSlug()
        + "\") { slug comments("
        + args
        + ") { edges { cursor node { id body createdAt updatedAt author { username } } } pageInfo"
        + " { hasNextPage hasPreviousPage startCursor endCursor } } } }";
  }

  @Test
  public void should_return_comments_of_an_article() {
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), isNull(), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(commentData), Direction.NEXT, true));
    when(profileQueryService.findByUsername(eq(user.getUsername()), isNull()))
        .thenReturn(Optional.of(TestHelper.profileDataFixture(user)));

    Map<String, Object> connection =
        dgsQueryExecutor.executeAndExtractJsonPath(
            commentsQuery("first: 10"), "data.article.comments");

    List<Map<String, Object>> edges = (List<Map<String, Object>>) connection.get("edges");
    assertThat(edges).hasSize(1);
    Map<String, Object> node = (Map<String, Object>) edges.get(0).get("node");
    assertThat(node.get("id")).isEqualTo(commentData.getId());
    assertThat(node.get("body")).isEqualTo(commentData.getBody());
    assertThat(((Map<String, Object>) node.get("author")).get("username"))
        .isEqualTo(user.getUsername());

    Map<String, Object> pageInfo = (Map<String, Object>) connection.get("pageInfo");
    assertThat(pageInfo.get("hasNextPage")).isEqualTo(true);
    assertThat(pageInfo.get("startCursor")).isEqualTo(commentData.getCursor().toString());
  }

  @Test
  public void should_page_comments_backward() {
    DateTime before = new DateTime().minusHours(2);
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), isNull(), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(commentData), Direction.PREV, true));
    when(profileQueryService.findByUsername(eq(user.getUsername()), isNull()))
        .thenReturn(Optional.of(TestHelper.profileDataFixture(user)));

    Map<String, Object> pageInfo =
        dgsQueryExecutor.executeAndExtractJsonPath(
            commentsQuery("last: 2, before: \"" + before.getMillis() + "\""),
            "data.article.comments.pageInfo");

    assertThat(pageInfo.get("hasPreviousPage")).isEqualTo(true);
    assertThat(pageInfo.get("hasNextPage")).isEqualTo(false);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(commentQueryService)
        .findByArticleIdWithCursor(eq(articleData.getId()), isNull(), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getCursor().getMillis()).isEqualTo(before.getMillis());
  }

  @Test
  public void should_return_empty_comments_with_null_cursors() {
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), isNull(), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    Map<String, Object> pageInfo =
        dgsQueryExecutor.executeAndExtractJsonPath(
            commentsQuery("first: 10"), "data.article.comments.pageInfo");

    assertThat(pageInfo.get("startCursor")).isNull();
    assertThat(pageInfo.get("endCursor")).isNull();
    assertThat(pageInfo.get("hasNextPage")).isEqualTo(false);
  }

  @Test
  public void should_pass_current_user_when_fetching_comments() {
    authenticate(user);
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), eq(user)))
        .thenReturn(Optional.of(articleData));
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), eq(user), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(commentData), Direction.NEXT, false));
    when(profileQueryService.findByUsername(eq(user.getUsername()), eq(user)))
        .thenReturn(Optional.of(TestHelper.profileDataFixture(user)));

    String id =
        dgsQueryExecutor.executeAndExtractJsonPath(
            commentsQuery("first: 10"), "data.article.comments.edges[0].node.id");

    assertThat(id).isEqualTo(commentData.getId());
  }

  @Test
  public void should_fail_comments_when_neither_first_nor_last_is_given() {
    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ article(slug: \""
                + articleData.getSlug()
                + "\") { comments { edges { node { id } } } } }");

    assertThat(result.getErrors()).isNotEmpty();
  }
}
