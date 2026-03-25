package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.readservice.CommentReadService;
import io.spring.infrastructure.mybatis.readservice.UserRelationshipQueryService;
import java.util.*;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CommentQueryServiceCoverageTest {

  private CommentReadService commentReadService;
  private UserRelationshipQueryService userRelationshipQueryService;
  private CommentQueryService commentQueryService;

  @BeforeEach
  void setUp() {
    commentReadService = mock(CommentReadService.class);
    userRelationshipQueryService = mock(UserRelationshipQueryService.class);
    commentQueryService =
        new CommentQueryService(commentReadService, userRelationshipQueryService);
  }

  @Test
  public void should_find_by_id_returns_empty_when_not_found() {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    when(commentReadService.findById("id1")).thenReturn(null);

    Optional<CommentData> result = commentQueryService.findById("id1", user);

    assertTrue(result.isEmpty());
  }

  @Test
  public void should_find_by_id_with_following() {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    CommentData commentData = createCommentData("id1");
    when(commentReadService.findById("id1")).thenReturn(commentData);
    when(userRelationshipQueryService.isUserFollowing(anyString(), anyString())).thenReturn(true);

    Optional<CommentData> result = commentQueryService.findById("id1", user);

    assertTrue(result.isPresent());
  }

  @Test
  public void should_find_by_article_id_empty() {
    when(commentReadService.findByArticleId("article1")).thenReturn(Collections.emptyList());

    List<CommentData> result = commentQueryService.findByArticleId("article1", null);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void should_find_by_article_id_without_user() {
    CommentData comment = createCommentData("id1");
    when(commentReadService.findByArticleId("article1")).thenReturn(Arrays.asList(comment));

    List<CommentData> result = commentQueryService.findByArticleId("article1", null);

    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  public void should_find_by_article_id_with_user() {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    CommentData comment = createCommentData("id1");
    when(commentReadService.findByArticleId("article1")).thenReturn(Arrays.asList(comment));
    when(userRelationshipQueryService.followingAuthors(anyString(), anyList()))
        .thenReturn(new HashSet<>(Arrays.asList("author-id")));

    List<CommentData> result = commentQueryService.findByArticleId("article1", user);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getProfileData().isFollowing());
  }

  @Test
  public void should_find_by_article_id_with_cursor_empty() {
    CursorPageParameter<DateTime> page =
        new CursorPageParameter<>(null, 20, CursorPager.Direction.NEXT);
    when(commentReadService.findByArticleIdWithCursor("article1", page))
        .thenReturn(Collections.emptyList());

    CursorPager<CommentData> result =
        commentQueryService.findByArticleIdWithCursor("article1", null, page);

    assertNotNull(result);
    assertTrue(result.getData().isEmpty());
  }

  @Test
  public void should_find_by_article_id_with_cursor_and_user() {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    CommentData comment = createCommentData("id1");
    CursorPageParameter<DateTime> page =
        new CursorPageParameter<>(null, 20, CursorPager.Direction.NEXT);
    when(commentReadService.findByArticleIdWithCursor("article1", page))
        .thenReturn(new ArrayList<>(Arrays.asList(comment)));
    when(userRelationshipQueryService.followingAuthors(anyString(), anyList()))
        .thenReturn(new HashSet<>(Arrays.asList("author-id")));

    CursorPager<CommentData> result =
        commentQueryService.findByArticleIdWithCursor("article1", user, page);

    assertNotNull(result);
    assertFalse(result.getData().isEmpty());
  }

  @Test
  public void should_find_by_article_id_with_cursor_without_user() {
    CommentData comment = createCommentData("id1");
    CursorPageParameter<DateTime> page =
        new CursorPageParameter<>(null, 20, CursorPager.Direction.NEXT);
    when(commentReadService.findByArticleIdWithCursor("article1", page))
        .thenReturn(new ArrayList<>(Arrays.asList(comment)));

    CursorPager<CommentData> result =
        commentQueryService.findByArticleIdWithCursor("article1", null, page);

    assertNotNull(result);
    assertFalse(result.getData().isEmpty());
  }

  @Test
  public void should_find_by_article_id_with_cursor_has_extra() {
    CommentData c1 = createCommentData("id1");
    CommentData c2 = createCommentData("id2");
    CursorPageParameter<DateTime> page =
        new CursorPageParameter<>(null, 1, CursorPager.Direction.NEXT);
    when(commentReadService.findByArticleIdWithCursor("article1", page))
        .thenReturn(new ArrayList<>(Arrays.asList(c1, c2)));

    CursorPager<CommentData> result =
        commentQueryService.findByArticleIdWithCursor("article1", null, page);

    assertNotNull(result);
    assertTrue(result.hasNext());
  }

  @Test
  public void should_find_by_article_id_with_cursor_prev_direction() {
    CommentData c1 = createCommentData("id1");
    CursorPageParameter<DateTime> page =
        new CursorPageParameter<>(null, 20, CursorPager.Direction.PREV);
    when(commentReadService.findByArticleIdWithCursor("article1", page))
        .thenReturn(new ArrayList<>(Arrays.asList(c1)));

    CursorPager<CommentData> result =
        commentQueryService.findByArticleIdWithCursor("article1", null, page);

    assertNotNull(result);
  }

  private CommentData createCommentData(String id) {
    ProfileData profileData = new ProfileData("author-id", "author", "bio", "img", false);
    return new CommentData(id, "comment body", "article1", new DateTime(), new DateTime(), profileData);
  }
}
