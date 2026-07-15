package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.CommentsConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class CommentDatafetcherTest {

  private CommentQueryService commentQueryService;
  private CommentDatafetcher commentDatafetcher;

  @BeforeEach
  void setUp() {
    commentQueryService = mock(CommentQueryService.class);
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private User authenticate() {
    User user = new User("user@example.com", "user", "password", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    return user;
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  private CommentData commentData(String id) {
    return new CommentData(
        id,
        "body",
        "article-id",
        new DateTime(),
        new DateTime(),
        new ProfileData("pid", "author", "bio", "image", false));
  }

  private ArticleData articleData(String id, String slug) {
    return new ArticleData(
        id,
        slug,
        "title",
        "description",
        "body",
        false,
        0,
        new DateTime(),
        new DateTime(),
        Arrays.asList("java"),
        new ProfileData("pid", "author", "bio", "image", false));
  }

  @Test
  void articleComments_throws_when_first_and_last_null() {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            commentDatafetcher.articleComments(
                null, null, null, null, new DgsDataFetchingEnvironment(dfe)));
  }

  @Test
  void articleComments_pages_next_when_first_present() {
    User current = authenticate();
    ArticleData articleData = articleData("article-1", "slug-1");
    Article article = Article.newBuilder().slug("slug-1").build();
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put("slug-1", articleData);

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<Article>getSource()).thenReturn(article);
    when(dfe.<Map<String, ArticleData>>getLocalContext()).thenReturn(localContext);
    when(commentQueryService.findByArticleIdWithCursor(eq("article-1"), eq(current), any()))
        .thenReturn(
            new CursorPager<>(Arrays.asList(commentData("comment-1")), Direction.NEXT, false));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(
            10, null, null, null, new DgsDataFetchingEnvironment(dfe));

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(commentQueryService)
        .findByArticleIdWithCursor(eq("article-1"), eq(current), captor.capture());
    assertEquals(Direction.NEXT, captor.getValue().getDirection());
    assertNull(captor.getValue().getCursor());
    assertEquals(1, result.getData().getEdges().size());
    assertEquals("comment-1", result.getData().getEdges().get(0).getNode().getId());
    assertTrue(((Map<?, ?>) result.getLocalContext()).containsKey("comment-1"));
  }

  @Test
  void articleComments_pages_prev_when_last_present() {
    User current = authenticate();
    ArticleData articleData = articleData("article-2", "slug-2");
    Article article = Article.newBuilder().slug("slug-2").build();
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put("slug-2", articleData);

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<Article>getSource()).thenReturn(article);
    when(dfe.<Map<String, ArticleData>>getLocalContext()).thenReturn(localContext);
    when(commentQueryService.findByArticleIdWithCursor(eq("article-2"), eq(current), any()))
        .thenReturn(
            new CursorPager<>(Arrays.asList(commentData("comment-2")), Direction.PREV, false));

    commentDatafetcher.articleComments(null, null, 10, "6000", new DgsDataFetchingEnvironment(dfe));

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(commentQueryService)
        .findByArticleIdWithCursor(eq("article-2"), eq(current), captor.capture());
    assertEquals(Direction.PREV, captor.getValue().getDirection());
    assertEquals(6000L, captor.getValue().getCursor().getMillis());
  }

  @Test
  void articleComments_uses_null_user_when_anonymous_and_handles_empty_page() {
    anonymous();
    ArticleData articleData = articleData("article-3", "slug-3");
    Article article = Article.newBuilder().slug("slug-3").build();
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put("slug-3", articleData);

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<Article>getSource()).thenReturn(article);
    when(dfe.<Map<String, ArticleData>>getLocalContext()).thenReturn(localContext);
    when(commentQueryService.findByArticleIdWithCursor(eq("article-3"), isNull(), any()))
        .thenReturn(new CursorPager<>(new ArrayList<>(), Direction.NEXT, false));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(
            10, null, null, null, new DgsDataFetchingEnvironment(dfe));

    assertTrue(result.getData().getEdges().isEmpty());
    assertNull(result.getData().getPageInfo().getStartCursor());
    assertNull(result.getData().getPageInfo().getEndCursor());
    verify(commentQueryService).findByArticleIdWithCursor(eq("article-3"), isNull(), any());
  }

  @Test
  void getComment_returns_data_and_local_context() {
    CommentData comment = commentData("comment-x");
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<CommentData>getLocalContext()).thenReturn(comment);

    DataFetcherResult<io.spring.graphql.types.Comment> result =
        commentDatafetcher.getComment(new DgsDataFetchingEnvironment(dfe));

    assertEquals("comment-x", result.getData().getId());
    assertEquals("body", result.getData().getBody());
    assertTrue(((Map<?, ?>) result.getLocalContext()).containsKey("comment-x"));
  }
}
