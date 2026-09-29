package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.application.CursorPager.Direction;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.readservice.CommentReadService;
import io.spring.infrastructure.mybatis.readservice.UserRelationshipQueryService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CommentQueryServiceUnitTest {
  private CommentReadService commentReadService;
  private UserRelationshipQueryService userRelationshipQueryService;
  private CommentQueryService service;
  private User user;

  @BeforeEach
  public void setUp() {
    commentReadService = mock(CommentReadService.class);
    userRelationshipQueryService = mock(UserRelationshipQueryService.class);
    service = new CommentQueryService(commentReadService, userRelationshipQueryService);
    user = new User("a@b.com", "user", "pass", "", "");
  }

  private CommentData comment(String id, String authorId) {
    DateTime now = new DateTime();
    return new CommentData(
        id, "body", "article", now, now, new ProfileData(authorId, "u", "", "", false));
  }

  private List<CommentData> comments(String... ids) {
    List<CommentData> list = new ArrayList<>();
    for (String id : ids) {
      list.add(comment(id, "author" + id));
    }
    return list;
  }

  @Test
  public void should_return_empty_when_comment_missing() {
    assertFalse(service.findById("missing", user).isPresent());
  }

  @Test
  public void should_skip_following_lookup_without_user_or_comments() {
    when(commentReadService.findByArticleId("article")).thenReturn(comments("1"));
    assertEquals(1, service.findByArticleId("article", null).size());

    when(commentReadService.findByArticleId("empty")).thenReturn(new ArrayList<>());
    assertTrue(service.findByArticleId("empty", user).isEmpty());

    verify(userRelationshipQueryService, never()).followingAuthors(any(), any());
  }

  @Test
  public void should_mark_followed_authors_in_article_comments() {
    when(commentReadService.findByArticleId("article")).thenReturn(comments("1", "2"));
    when(userRelationshipQueryService.followingAuthors(eq(user.getId()), anyList()))
        .thenReturn(new HashSet<>(Collections.singletonList("author1")));

    List<CommentData> result = service.findByArticleId("article", user);

    assertTrue(result.get(0).getProfileData().isFollowing());
    assertFalse(result.get(1).getProfileData().isFollowing());
  }

  @Test
  public void should_return_empty_cursor_pager_when_no_comments() {
    CursorPageParameter<DateTime> page = new CursorPageParameter<>(null, 2, Direction.NEXT);
    when(commentReadService.findByArticleIdWithCursor("article", page))
        .thenReturn(new ArrayList<>());

    CursorPager<CommentData> pager = service.findByArticleIdWithCursor("article", user, page);

    assertTrue(pager.getData().isEmpty());
    assertFalse(pager.hasNext());
  }

  @Test
  public void should_trim_extra_comment_and_mark_followed_authors() {
    CursorPageParameter<DateTime> page = new CursorPageParameter<>(null, 2, Direction.NEXT);
    when(commentReadService.findByArticleIdWithCursor("article", page))
        .thenReturn(comments("1", "2", "3"));
    when(userRelationshipQueryService.followingAuthors(eq(user.getId()), anyList()))
        .thenReturn(new HashSet<>(Collections.singletonList("author2")));

    CursorPager<CommentData> pager = service.findByArticleIdWithCursor("article", user, page);

    assertEquals(2, pager.getData().size());
    assertTrue(pager.hasNext());
    assertFalse(pager.getData().get(0).getProfileData().isFollowing());
    assertTrue(pager.getData().get(1).getProfileData().isFollowing());
  }

  @Test
  public void should_reverse_comments_for_previous_page_without_user() {
    CursorPageParameter<DateTime> page = new CursorPageParameter<>(null, 5, Direction.PREV);
    when(commentReadService.findByArticleIdWithCursor("article", page))
        .thenReturn(comments("1", "2"));

    CursorPager<CommentData> pager = service.findByArticleIdWithCursor("article", null, page);

    assertEquals(
        Arrays.asList("2", "1"),
        Arrays.asList(pager.getData().get(0).getId(), pager.getData().get(1).getId()));
    assertFalse(pager.hasPrevious());
    verify(userRelationshipQueryService, never()).followingAuthors(any(), any());
  }
}
