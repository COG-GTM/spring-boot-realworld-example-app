package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.CommentsConnection;
import java.util.*;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class CommentDatafetcherTest {

  private CommentQueryService commentQueryService;
  private CommentDatafetcher commentDatafetcher;
  private User user;

  @BeforeEach
  void setUp() {
    commentQueryService = mock(CommentQueryService.class);
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
    user = new User("a@b.com", "testuser", "pass", "", "");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_get_comment_from_payload() {
    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("pid", "testuser", "bio", "img", false);
    CommentData commentData = new CommentData("c1", "body", "a1", now, now, profile);
    when(delegate.getLocalContext()).thenReturn(commentData);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dfe);
    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals("c1", result.getData().getId());
  }

  @Test
  public void should_get_article_comments_with_first() {
    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    Article article = Article.newBuilder().slug("slug1").build();
    when(delegate.getSource()).thenReturn(article);
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("pid", "testuser", "bio", "img", false);
    ArticleData articleData = new ArticleData("a1", "slug1", "Title", "desc", "body", false, 0, now, now, Collections.emptyList(), profile);
    Map<String, ArticleData> map = new HashMap<>();
    map.put("slug1", articleData);
    when(delegate.getLocalContext()).thenReturn(map);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);

    CommentData commentData = new CommentData("c1", "body", "a1", now, now, profile);
    CursorPager<CommentData> pager = new CursorPager<>(Arrays.asList(commentData), Direction.NEXT, true);
    when(commentQueryService.findByArticleIdWithCursor(any(), any(), any())).thenReturn(pager);

    DataFetcherResult<CommentsConnection> result = commentDatafetcher.articleComments(10, null, null, null, dfe);
    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  public void should_get_article_comments_with_last() {
    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    Article article = Article.newBuilder().slug("slug1").build();
    when(delegate.getSource()).thenReturn(article);
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("pid", "testuser", "bio", "img", false);
    ArticleData articleData = new ArticleData("a1", "slug1", "Title", "desc", "body", false, 0, now, now, Collections.emptyList(), profile);
    Map<String, ArticleData> map = new HashMap<>();
    map.put("slug1", articleData);
    when(delegate.getLocalContext()).thenReturn(map);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);

    CursorPager<CommentData> pager = new CursorPager<>(Collections.emptyList(), Direction.PREV, false);
    when(commentQueryService.findByArticleIdWithCursor(any(), any(), any())).thenReturn(pager);

    DataFetcherResult<CommentsConnection> result = commentDatafetcher.articleComments(null, null, 5, null, dfe);
    assertNotNull(result);
    assertEquals(0, result.getData().getEdges().size());
  }

  @Test
  public void should_throw_when_article_comments_missing_first_and_last() {
    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);
    assertThrows(IllegalArgumentException.class, () -> commentDatafetcher.articleComments(null, null, null, null, dfe));
  }
}
