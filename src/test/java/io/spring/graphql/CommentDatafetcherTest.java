package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.CommentsConnection;
import java.util.*;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class CommentDatafetcherTest {

  @Mock private CommentQueryService commentQueryService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private DgsDataFetchingEnvironment dfe;

  private CommentDatafetcher commentDatafetcher;
  private User currentUser;

  @BeforeEach
  void setUp() {
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
    currentUser = new User("test@example.com", "testuser", "password", "", "");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(currentUser, null);
    SecurityContextHolder.getContext().setAuthentication(auth);
    dfe = new DgsDataFetchingEnvironment(dataFetchingEnvironment);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_get_comment_from_payload() {
    CommentData commentData =
        new CommentData(
            "commentId",
            "comment body",
            "articleId",
            new DateTime(),
            new DateTime(),
            new ProfileData("authorId", "author", "bio", "img", false));

    when(dataFetchingEnvironment.getLocalContext()).thenReturn(commentData);

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dfe);

    assertNotNull(result);
    assertEquals("commentId", result.getData().getId());
    assertEquals("comment body", result.getData().getBody());
  }

  @Test
  void should_get_article_comments_with_first_param() {
    Article article = Article.newBuilder().slug("test-slug").build();
    ArticleData articleData =
        new ArticleData(
            "articleId",
            "test-slug",
            "Title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            new ProfileData("authorId", "author", "bio", "img", false));
    Map<String, ArticleData> map = new HashMap<>();
    map.put("test-slug", articleData);

    CursorPager<CommentData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    when(dataFetchingEnvironment.getSource()).thenReturn(article);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(map);
    when(commentQueryService.findByArticleIdWithCursor(
            eq("articleId"), any(), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertTrue(result.getData().getEdges().isEmpty());
  }

  @Test
  void should_get_article_comments_with_last_param() {
    Article article = Article.newBuilder().slug("test-slug").build();
    ArticleData articleData =
        new ArticleData(
            "articleId",
            "test-slug",
            "Title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            new ProfileData("authorId", "author", "bio", "img", false));
    Map<String, ArticleData> map = new HashMap<>();
    map.put("test-slug", articleData);

    CursorPager<CommentData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.PREV, false);

    when(dataFetchingEnvironment.getSource()).thenReturn(article);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(map);
    when(commentQueryService.findByArticleIdWithCursor(
            eq("articleId"), any(), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(null, null, 5, null, dfe);

    assertNotNull(result);
  }

  @Test
  void should_throw_when_article_comments_has_no_first_or_last() {
    assertThrows(
        IllegalArgumentException.class,
        () -> commentDatafetcher.articleComments(null, null, null, null, dfe));
  }

  @Test
  void should_get_article_comments_with_data() {
    Article article = Article.newBuilder().slug("test-slug").build();
    ArticleData articleData =
        new ArticleData(
            "articleId",
            "test-slug",
            "Title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            new ProfileData("authorId", "author", "bio", "img", false));
    Map<String, ArticleData> map = new HashMap<>();
    map.put("test-slug", articleData);

    CommentData comment =
        new CommentData(
            "cid1",
            "comment body",
            "articleId",
            new DateTime(),
            new DateTime(),
            new ProfileData("authorId", "author", "bio", "img", false));
    CursorPager<CommentData> pager =
        new CursorPager<>(Arrays.asList(comment), Direction.NEXT, true);

    when(dataFetchingEnvironment.getSource()).thenReturn(article);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(map);
    when(commentQueryService.findByArticleIdWithCursor(
            eq("articleId"), any(), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dfe);

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
    assertEquals("cid1", result.getData().getEdges().get(0).getNode().getId());
  }
}
