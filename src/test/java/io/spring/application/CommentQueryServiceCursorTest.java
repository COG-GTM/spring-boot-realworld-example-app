package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.spring.application.CursorPager.Direction;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.readservice.CommentReadService;
import io.spring.infrastructure.mybatis.readservice.UserRelationshipQueryService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommentQueryServiceCursorTest {

  @Mock private CommentReadService commentReadService;
  @Mock private UserRelationshipQueryService userRelationshipQueryService;

  private CommentQueryService commentQueryService;
  private final User currentUser = new User("john@example.com", "john", "123", "", "");

  @BeforeEach
  public void setUp() {
    commentQueryService = new CommentQueryService(commentReadService, userRelationshipQueryService);
  }

  private CommentData commentData(String id, String authorId) {
    return new CommentData(
        id,
        "body",
        "articleId",
        new DateTime(1000L),
        new DateTime(1000L),
        new ProfileData(authorId, "author", "bio", "image", false));
  }

  @Test
  public void should_return_empty_when_comment_is_not_found() {
    when(commentReadService.findById("not-exist")).thenReturn(null);

    assertThat(commentQueryService.findById("not-exist", currentUser)).isEmpty();
  }

  @Test
  public void should_set_following_flag_when_finding_comment_by_id() {
    CommentData data = commentData("1", "authorId");
    when(commentReadService.findById("1")).thenReturn(data);
    when(userRelationshipQueryService.isUserFollowing(currentUser.getId(), "authorId"))
        .thenReturn(true);

    assertThat(commentQueryService.findById("1", currentUser))
        .get()
        .extracting(comment -> comment.getProfileData().isFollowing())
        .isEqualTo(true);
  }

  @Test
  public void should_not_query_relationships_when_article_has_no_comment() {
    when(commentReadService.findByArticleId("articleId")).thenReturn(new ArrayList<>());

    assertThat(commentQueryService.findByArticleId("articleId", currentUser)).isEmpty();
  }

  @Test
  public void should_mark_followed_authors_of_article_comments() {
    List<CommentData> comments =
        new ArrayList<>(Arrays.asList(commentData("1", "followed"), commentData("2", "other")));
    when(commentReadService.findByArticleId("articleId")).thenReturn(comments);
    when(userRelationshipQueryService.followingAuthors(eq(currentUser.getId()), anyList()))
        .thenReturn(new HashSet<>(Arrays.asList("followed")));

    List<CommentData> found = commentQueryService.findByArticleId("articleId", currentUser);

    assertThat(found.get(0).getProfileData().isFollowing()).isTrue();
    assertThat(found.get(1).getProfileData().isFollowing()).isFalse();
  }

  @Test
  public void should_return_empty_cursor_pager_when_article_has_no_comment() {
    CursorPageParameter<DateTime> page = new CursorPageParameter<>(null, 2, Direction.NEXT);
    when(commentReadService.findByArticleIdWithCursor("articleId", page))
        .thenReturn(new ArrayList<>());

    CursorPager<CommentData> pager =
        commentQueryService.findByArticleIdWithCursor("articleId", currentUser, page);

    assertThat(pager.getData()).isEmpty();
    assertThat(pager.hasNext()).isFalse();
  }

  @Test
  public void should_drop_the_extra_comment_and_flag_next_page() {
    CursorPageParameter<DateTime> page = new CursorPageParameter<>(null, 1, Direction.NEXT);
    when(commentReadService.findByArticleIdWithCursor("articleId", page))
        .thenReturn(
            new ArrayList<>(
                Arrays.asList(commentData("1", "followed"), commentData("2", "other"))));
    when(userRelationshipQueryService.followingAuthors(eq(currentUser.getId()), anyList()))
        .thenReturn(new HashSet<>(Arrays.asList("followed")));

    CursorPager<CommentData> pager =
        commentQueryService.findByArticleIdWithCursor("articleId", currentUser, page);

    assertThat(pager.getData()).extracting(CommentData::getId).containsExactly("1");
    assertThat(pager.getData().get(0).getProfileData().isFollowing()).isTrue();
    assertThat(pager.hasNext()).isTrue();
  }

  @Test
  public void should_reverse_comments_when_paging_backwards_for_anonymous_user() {
    CursorPageParameter<DateTime> page = new CursorPageParameter<>(null, 5, Direction.PREV);
    when(commentReadService.findByArticleIdWithCursor("articleId", page))
        .thenReturn(
            new ArrayList<>(Arrays.asList(commentData("2", "other"), commentData("1", "other"))));

    CursorPager<CommentData> pager =
        commentQueryService.findByArticleIdWithCursor("articleId", null, page);

    assertThat(pager.getData()).extracting(CommentData::getId).containsExactly("1", "2");
    assertThat(pager.hasPrevious()).isFalse();
  }
}
