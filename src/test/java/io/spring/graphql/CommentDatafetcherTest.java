package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.CommentQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import java.util.Arrays;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class CommentDatafetcherTest {

  @Mock
  private CommentQueryService commentQueryService;

  private CommentDatafetcher commentDatafetcher;

  private User user;
  private CommentData commentData;
  private ProfileData profileData;

  @BeforeEach
  void setUp() {
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    
    DateTime now = new DateTime();
    commentData = new CommentData(
        "comment-id",
        "This is a test comment",
        "article-id",
        now,
        now,
        profileData
    );
    
    setAnonymousAuthentication();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAnonymousAuthentication() {
    AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
        "anonymous", "anonymousUser", 
        java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);
  }

  @Test
  void shouldCreateCommentDatafetcher() {
    assertThat(commentDatafetcher).isNotNull();
  }

  @Test
  void shouldHaveCommentQueryServiceInjected() {
    assertThat(commentQueryService).isNotNull();
  }

  @Test
  void shouldCreateCursorPagerWithComments() {
    CursorPager<CommentData> cursorPager = new CursorPager<>(
        Arrays.asList(commentData),
        Direction.NEXT,
        false
    );
    
    assertThat(cursorPager.getData()).hasSize(1);
    assertThat(cursorPager.getData().get(0).getBody()).isEqualTo("This is a test comment");
    assertThat(cursorPager.hasNext()).isFalse();
  }

  @Test
  void shouldCreateCursorPagerWithHasNext() {
    CursorPager<CommentData> cursorPager = new CursorPager<>(
        Arrays.asList(commentData),
        Direction.NEXT,
        true
    );
    
    assertThat(cursorPager.hasNext()).isTrue();
  }

  @Test
  void shouldCreateCursorPagerWithPrevDirection() {
    CursorPager<CommentData> cursorPager = new CursorPager<>(
        Arrays.asList(commentData),
        Direction.PREV,
        false
    );
    
    assertThat(cursorPager.getData()).hasSize(1);
    assertThat(cursorPager.hasPrevious()).isFalse();
  }

  @Test
  void shouldCreateCommentDataWithCorrectFields() {
    assertThat(commentData.getId()).isEqualTo("comment-id");
    assertThat(commentData.getBody()).isEqualTo("This is a test comment");
    assertThat(commentData.getArticleId()).isEqualTo("article-id");
    assertThat(commentData.getProfileData()).isEqualTo(profileData);
  }
}
