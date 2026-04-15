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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommentQueryServiceMockTest {

  @Mock private CommentReadService commentReadService;
  @Mock private UserRelationshipQueryService userRelationshipQueryService;

  private CommentQueryService commentQueryService;

  @BeforeEach
  void setUp() {
    commentQueryService = new CommentQueryService(commentReadService, userRelationshipQueryService);
  }

  @Test
  void should_return_empty_when_comment_not_found() {
    when(commentReadService.findById("nonexistent")).thenReturn(null);
    User user = new User("user@example.com", "user", "pass", "", "");

    Optional<CommentData> result = commentQueryService.findById("nonexistent", user);

    assertFalse(result.isPresent());
  }

  @Test
  void should_find_comment_by_id_and_set_following() {
    User user = new User("user@example.com", "user", "pass", "", "");
    ProfileData profileData = new ProfileData("authorId", "author", "bio", "img", false);
    CommentData commentData =
        new CommentData(
            "commentId1",
            "comment body",
            "articleId1",
            new DateTime(),
            new DateTime(),
            profileData);
    when(commentReadService.findById("commentId1")).thenReturn(commentData);
    when(userRelationshipQueryService.isUserFollowing(user.getId(), "authorId")).thenReturn(true);

    Optional<CommentData> result = commentQueryService.findById("commentId1", user);

    assertTrue(result.isPresent());
    assertTrue(result.get().getProfileData().isFollowing());
  }

  @Test
  void should_return_empty_list_when_no_comments() {
    when(commentReadService.findByArticleId("articleId1")).thenReturn(Collections.emptyList());

    List<CommentData> result = commentQueryService.findByArticleId("articleId1", null);

    assertTrue(result.isEmpty());
  }

  @Test
  void should_return_comments_with_following_status() {
    User user = new User("user@example.com", "user", "pass", "", "");
    ProfileData profileData = new ProfileData("authorId", "author", "bio", "img", false);
    CommentData commentData =
        new CommentData(
            "commentId1",
            "comment body",
            "articleId1",
            new DateTime(),
            new DateTime(),
            profileData);
    when(commentReadService.findByArticleId("articleId1")).thenReturn(Arrays.asList(commentData));
    when(userRelationshipQueryService.followingAuthors(eq(user.getId()), anyList()))
        .thenReturn(new HashSet<>(Arrays.asList("authorId")));

    List<CommentData> result = commentQueryService.findByArticleId("articleId1", user);

    assertEquals(1, result.size());
    assertTrue(result.get(0).getProfileData().isFollowing());
  }

  @Test
  void should_return_comments_without_following_when_no_user() {
    ProfileData profileData = new ProfileData("authorId", "author", "bio", "img", false);
    CommentData commentData =
        new CommentData(
            "commentId1",
            "comment body",
            "articleId1",
            new DateTime(),
            new DateTime(),
            profileData);
    when(commentReadService.findByArticleId("articleId1")).thenReturn(Arrays.asList(commentData));

    List<CommentData> result = commentQueryService.findByArticleId("articleId1", null);

    assertEquals(1, result.size());
    assertFalse(result.get(0).getProfileData().isFollowing());
  }

  @Test
  void should_return_empty_cursor_pager_when_no_comments() {
    when(commentReadService.findByArticleIdWithCursor(eq("articleId1"), any()))
        .thenReturn(Collections.emptyList());

    CursorPager<CommentData> result =
        commentQueryService.findByArticleIdWithCursor(
            "articleId1", null, new CursorPageParameter<>(null, 10, CursorPager.Direction.NEXT));

    assertTrue(result.getData().isEmpty());
    assertFalse(result.hasNext());
  }
}
