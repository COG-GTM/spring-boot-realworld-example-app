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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CommentDatafetcherTest {

  @Mock private CommentQueryService commentQueryService;
  @Mock private DataFetchingEnvironment dfe;

  private CommentDatafetcher commentDatafetcher;
  private User user;
  private ProfileData profileData;

  @BeforeEach
  void setUp() {
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    profileData =
        new ProfileData(
            user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private DgsDataFetchingEnvironment dgsDfe() {
    return new DgsDataFetchingEnvironment(dfe);
  }

  private void setAuthenticatedUser(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(u, null, AuthorityUtils.NO_AUTHORITIES));
  }

  private void setAnonymousUser() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  private CommentData createCommentData(String id) {
    DateTime now = new DateTime();
    return new CommentData(id, "comment body " + id, "article-id-1", now, now, profileData);
  }

  // --- getComment tests ---

  @Test
  void getComment_returnsCommentFromLocalContext() {
    CommentData commentData = createCommentData("c1");
    when(dfe.getLocalContext()).thenReturn(commentData);

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dgsDfe());

    assertNotNull(result);
    assertEquals("c1", result.getData().getId());
    assertEquals("comment body c1", result.getData().getBody());
    assertNotNull(result.getData().getCreatedAt());
    assertNotNull(result.getData().getUpdatedAt());
  }

  @Test
  void getComment_setsLocalContextWithCommentMap() {
    CommentData commentData = createCommentData("c2");
    when(dfe.getLocalContext()).thenReturn(commentData);

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dgsDfe());

    @SuppressWarnings("unchecked")
    Map<String, Object> localCtx = (Map<String, Object>) result.getLocalContext();
    assertNotNull(localCtx);
    assertEquals(commentData, localCtx.get("c2"));
  }

  // --- articleComments tests ---

  @Test
  void articleComments_withFirstParam_returnsCommentsConnection() {
    setAuthenticatedUser(user);
    Article article = Article.newBuilder().slug("test-slug").build();
    ArticleData articleData =
        new ArticleData(
            "art-id",
            "test-slug",
            "title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            new ArrayList<>(),
            profileData);
    Map<String, ArticleData> contextMap = new HashMap<>();
    contextMap.put("test-slug", articleData);

    when(dfe.getSource()).thenReturn(article);
    when(dfe.getLocalContext()).thenReturn(contextMap);

    List<CommentData> comments = Arrays.asList(createCommentData("c1"), createCommentData("c2"));
    CursorPager<CommentData> pager = new CursorPager<>(comments, Direction.NEXT, true);

    when(commentQueryService.findByArticleIdWithCursor(
            eq("art-id"), eq(user), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dgsDfe());

    assertNotNull(result);
    assertEquals(2, result.getData().getEdges().size());
    assertTrue(result.getData().getPageInfo().isHasNextPage());
    assertFalse(result.getData().getPageInfo().isHasPreviousPage());
  }

  @Test
  void articleComments_withLastParam_usesPrevDirection() {
    setAnonymousUser();
    Article article = Article.newBuilder().slug("test-slug").build();
    ArticleData articleData =
        new ArticleData(
            "art-id",
            "test-slug",
            "title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            new ArrayList<>(),
            profileData);
    Map<String, ArticleData> contextMap = new HashMap<>();
    contextMap.put("test-slug", articleData);

    when(dfe.getSource()).thenReturn(article);
    when(dfe.getLocalContext()).thenReturn(contextMap);

    CursorPager<CommentData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.PREV, false);

    when(commentQueryService.findByArticleIdWithCursor(
            eq("art-id"), isNull(), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(null, null, 5, null, dgsDfe());

    assertNotNull(result);
    assertTrue(result.getData().getEdges().isEmpty());
  }

  @Test
  void articleComments_noFirstOrLast_throwsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> commentDatafetcher.articleComments(null, null, null, null, dgsDfe()));
  }

  @Test
  void articleComments_emptyResult_returnsEmptyConnection() {
    setAnonymousUser();
    Article article = Article.newBuilder().slug("test-slug").build();
    ArticleData articleData =
        new ArticleData(
            "art-id",
            "test-slug",
            "title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            new ArrayList<>(),
            profileData);
    Map<String, ArticleData> contextMap = new HashMap<>();
    contextMap.put("test-slug", articleData);

    when(dfe.getSource()).thenReturn(article);
    when(dfe.getLocalContext()).thenReturn(contextMap);

    CursorPager<CommentData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    when(commentQueryService.findByArticleIdWithCursor(
            eq("art-id"), isNull(), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dgsDfe());

    assertTrue(result.getData().getEdges().isEmpty());
    assertNull(result.getData().getPageInfo().getStartCursor());
    assertNull(result.getData().getPageInfo().getEndCursor());
  }

  @Test
  void articleComments_setsLocalContextWithCommentIdMap() {
    setAuthenticatedUser(user);
    Article article = Article.newBuilder().slug("test-slug").build();
    ArticleData articleData =
        new ArticleData(
            "art-id",
            "test-slug",
            "title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            new ArrayList<>(),
            profileData);
    Map<String, ArticleData> contextMap = new HashMap<>();
    contextMap.put("test-slug", articleData);

    when(dfe.getSource()).thenReturn(article);
    when(dfe.getLocalContext()).thenReturn(contextMap);

    CommentData comment = createCommentData("c1");
    CursorPager<CommentData> pager =
        new CursorPager<>(Arrays.asList(comment), Direction.NEXT, false);

    when(commentQueryService.findByArticleIdWithCursor(
            eq("art-id"), eq(user), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dgsDfe());

    @SuppressWarnings("unchecked")
    Map<String, CommentData> localCtx = (Map<String, CommentData>) result.getLocalContext();
    assertNotNull(localCtx);
    assertEquals(comment, localCtx.get("c1"));
  }

  @Test
  void articleComments_cursorValues_areStringifiedTimestamps() {
    setAuthenticatedUser(user);
    Article article = Article.newBuilder().slug("test-slug").build();
    ArticleData articleData =
        new ArticleData(
            "art-id",
            "test-slug",
            "title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            new ArrayList<>(),
            profileData);
    Map<String, ArticleData> contextMap = new HashMap<>();
    contextMap.put("test-slug", articleData);

    when(dfe.getSource()).thenReturn(article);
    when(dfe.getLocalContext()).thenReturn(contextMap);

    CommentData comment = createCommentData("c1");
    CursorPager<CommentData> pager =
        new CursorPager<>(Arrays.asList(comment), Direction.NEXT, false);

    when(commentQueryService.findByArticleIdWithCursor(
            eq("art-id"), eq(user), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dgsDfe());

    String cursor = result.getData().getEdges().get(0).getCursor();
    assertDoesNotThrow(() -> Long.parseLong(cursor));
  }

  @Test
  void articleComments_pageInfo_hasPrevious() {
    setAuthenticatedUser(user);
    Article article = Article.newBuilder().slug("test-slug").build();
    ArticleData articleData =
        new ArticleData(
            "art-id",
            "test-slug",
            "title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            new ArrayList<>(),
            profileData);
    Map<String, ArticleData> contextMap = new HashMap<>();
    contextMap.put("test-slug", articleData);

    when(dfe.getSource()).thenReturn(article);
    when(dfe.getLocalContext()).thenReturn(contextMap);

    List<CommentData> comments = Arrays.asList(createCommentData("c1"));
    CursorPager<CommentData> pager = new CursorPager<>(comments, Direction.PREV, true);

    when(commentQueryService.findByArticleIdWithCursor(
            eq("art-id"), eq(user), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(null, null, 5, null, dgsDfe());

    assertFalse(result.getData().getPageInfo().isHasNextPage());
    assertTrue(result.getData().getPageInfo().isHasPreviousPage());
  }
}
