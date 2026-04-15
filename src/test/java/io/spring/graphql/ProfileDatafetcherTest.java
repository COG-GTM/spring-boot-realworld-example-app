package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.Profile;
import io.spring.graphql.types.ProfilePayload;
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
public class ProfileDatafetcherTest {

  @Mock private ProfileQueryService profileQueryService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private ProfileDatafetcher profileDatafetcher;
  private User currentUser;

  @BeforeEach
  void setUp() {
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);
    currentUser = new User("test@example.com", "testuser", "password", "", "");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(currentUser, null);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_get_user_profile() {
    User user = new User("profile@example.com", "profileuser", "pass", "bio", "img");
    ProfileData profileData = new ProfileData("userId", "profileuser", "bio", "img", false);

    when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);
    when(profileQueryService.findByUsername(eq("profileuser"), any()))
        .thenReturn(Optional.of(profileData));

    Profile result = profileDatafetcher.getUserProfile(dataFetchingEnvironment);

    assertNotNull(result);
    assertEquals("profileuser", result.getUsername());
    assertEquals("bio", result.getBio());
    assertEquals("img", result.getImage());
  }

  @Test
  void should_get_article_author_profile() {
    Article article = Article.newBuilder().slug("test-slug").build();
    ArticleData articleData =
        new ArticleData(
            "id1",
            "test-slug",
            "Title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            new ProfileData("authorId", "authoruser", "bio", "img", false));
    Map<String, ArticleData> map = new HashMap<>();
    map.put("test-slug", articleData);
    ProfileData profileData = new ProfileData("authorId", "authoruser", "bio", "img", false);

    when(dataFetchingEnvironment.getLocalContext()).thenReturn(map);
    when(dataFetchingEnvironment.getSource()).thenReturn(article);
    when(profileQueryService.findByUsername(eq("authoruser"), any()))
        .thenReturn(Optional.of(profileData));

    Profile result = profileDatafetcher.getAuthor(dataFetchingEnvironment);

    assertNotNull(result);
    assertEquals("authoruser", result.getUsername());
  }

  @Test
  void should_get_comment_author_profile() {
    Comment comment = Comment.newBuilder().id("commentId").build();
    CommentData commentData =
        new CommentData(
            "commentId",
            "body",
            "articleId",
            new DateTime(),
            new DateTime(),
            new ProfileData("authorId", "commentauthor", "bio", "img", false));
    Map<String, CommentData> map = new HashMap<>();
    map.put("commentId", commentData);
    ProfileData profileData = new ProfileData("authorId", "commentauthor", "bio", "img", false);

    when(dataFetchingEnvironment.getSource()).thenReturn(comment);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(map);
    when(profileQueryService.findByUsername(eq("commentauthor"), any()))
        .thenReturn(Optional.of(profileData));

    Profile result = profileDatafetcher.getCommentAuthor(dataFetchingEnvironment);

    assertNotNull(result);
    assertEquals("commentauthor", result.getUsername());
  }

  @Test
  void should_query_profile_by_username() {
    ProfileData profileData = new ProfileData("userId", "someuser", "bio", "img", true);

    when(dataFetchingEnvironment.getArgument("username")).thenReturn("someuser");
    when(profileQueryService.findByUsername(eq("someuser"), any()))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result = profileDatafetcher.queryProfile("someuser", dataFetchingEnvironment);

    assertNotNull(result);
    assertNotNull(result.getProfile());
    assertEquals("someuser", result.getProfile().getUsername());
    assertTrue(result.getProfile().getFollowing());
  }

  @Test
  void should_throw_when_profile_not_found() {
    when(dataFetchingEnvironment.getArgument("username")).thenReturn("nonexistent");
    when(profileQueryService.findByUsername(eq("nonexistent"), any())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> profileDatafetcher.queryProfile("nonexistent", dataFetchingEnvironment));
  }
}
