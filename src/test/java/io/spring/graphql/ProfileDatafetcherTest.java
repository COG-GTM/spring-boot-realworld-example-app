package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ProfileDatafetcherTest {

  @Mock private ProfileQueryService profileQueryService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private ProfileDatafetcher profileDatafetcher;

  @BeforeEach
  void setUp() {
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getUserProfile_should_return_profile_for_user() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image");
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);

    ProfileData profileData = new ProfileData(user.getId(), "testuser", "bio", "image", false);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    // Set up authenticated user for SecurityUtil.getCurrentUser()
    TestingAuthenticationToken authentication =
        new TestingAuthenticationToken(user, null, "ROLE_USER");
    SecurityContextHolder.getContext().setAuthentication(authentication);

    Profile result = profileDatafetcher.getUserProfile(dataFetchingEnvironment);

    assertNotNull(result);
    assertEquals("testuser", result.getUsername());
    assertEquals("bio", result.getBio());
    assertEquals("image", result.getImage());
    assertFalse(result.getFollowing());
  }

  @Test
  void getUserProfile_should_throw_when_profile_not_found() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image");
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);

    when(profileQueryService.findByUsername(eq("testuser"), any())).thenReturn(Optional.empty());

    TestingAuthenticationToken authentication =
        new TestingAuthenticationToken(user, null, "ROLE_USER");
    SecurityContextHolder.getContext().setAuthentication(authentication);

    assertThrows(
        ResourceNotFoundException.class,
        () -> profileDatafetcher.getUserProfile(dataFetchingEnvironment));
  }

  @Test
  void getUserProfile_should_work_when_anonymous() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image");
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);

    ProfileData profileData = new ProfileData(user.getId(), "testuser", "bio", "image", false);
    when(profileQueryService.findByUsername(eq("testuser"), isNull()))
        .thenReturn(Optional.of(profileData));

    AnonymousAuthenticationToken anonymousToken =
        new AnonymousAuthenticationToken(
            "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);

    Profile result = profileDatafetcher.getUserProfile(dataFetchingEnvironment);

    assertNotNull(result);
    assertEquals("testuser", result.getUsername());
  }

  @Test
  void getAuthor_should_return_profile_for_article_author() {
    String slug = "test-article";
    ProfileData authorProfile = new ProfileData("author-id", "authoruser", "bio", "image", false);
    ArticleData articleData = mock(ArticleData.class);
    when(articleData.getProfileData()).thenReturn(authorProfile);

    Map<String, ArticleData> map = new HashMap<>();
    map.put(slug, articleData);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(map);

    Article article = Article.newBuilder().slug(slug).build();
    when(dataFetchingEnvironment.getSource()).thenReturn(article);

    ProfileData profileData =
        new ProfileData("author-id", "authoruser", "author bio", "author image", true);
    when(profileQueryService.findByUsername(eq("authoruser"), any()))
        .thenReturn(Optional.of(profileData));

    // Set up anonymous context
    AnonymousAuthenticationToken anonymousToken =
        new AnonymousAuthenticationToken(
            "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);

    Profile result = profileDatafetcher.getAuthor(dataFetchingEnvironment);

    assertNotNull(result);
    assertEquals("authoruser", result.getUsername());
    assertEquals("author bio", result.getBio());
    assertEquals("author image", result.getImage());
    assertTrue(result.getFollowing());
  }

  @Test
  void getCommentAuthor_should_return_profile_for_comment_author() {
    String commentId = "comment-1";
    ProfileData commentAuthorProfile =
        new ProfileData("commenter-id", "commenter", "bio", "image", false);
    CommentData commentData = mock(CommentData.class);
    when(commentData.getProfileData()).thenReturn(commentAuthorProfile);

    Map<String, CommentData> map = new HashMap<>();
    map.put(commentId, commentData);

    Comment comment = Comment.newBuilder().id(commentId).build();
    when(dataFetchingEnvironment.getSource()).thenReturn(comment);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(map);

    ProfileData profileData =
        new ProfileData("commenter-id", "commenter", "commenter bio", "commenter image", false);
    when(profileQueryService.findByUsername(eq("commenter"), any()))
        .thenReturn(Optional.of(profileData));

    AnonymousAuthenticationToken anonymousToken =
        new AnonymousAuthenticationToken(
            "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);

    Profile result = profileDatafetcher.getCommentAuthor(dataFetchingEnvironment);

    assertNotNull(result);
    assertEquals("commenter", result.getUsername());
    assertEquals("commenter bio", result.getBio());
  }

  @Test
  void queryProfile_should_return_profile_payload() {
    String username = "targetuser";
    when(dataFetchingEnvironment.getArgument("username")).thenReturn(username);

    ProfileData profileData =
        new ProfileData("target-id", "targetuser", "target bio", "target image", false);
    when(profileQueryService.findByUsername(eq("targetuser"), any()))
        .thenReturn(Optional.of(profileData));

    AnonymousAuthenticationToken anonymousToken =
        new AnonymousAuthenticationToken(
            "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);

    ProfilePayload result = profileDatafetcher.queryProfile(username, dataFetchingEnvironment);

    assertNotNull(result);
    assertNotNull(result.getProfile());
    assertEquals("targetuser", result.getProfile().getUsername());
    assertEquals("target bio", result.getProfile().getBio());
    assertEquals("target image", result.getProfile().getImage());
    assertFalse(result.getProfile().getFollowing());
  }

  @Test
  void queryProfile_should_throw_when_profile_not_found() {
    String username = "nonexistent";
    when(dataFetchingEnvironment.getArgument("username")).thenReturn(username);

    when(profileQueryService.findByUsername(eq("nonexistent"), any())).thenReturn(Optional.empty());

    AnonymousAuthenticationToken anonymousToken =
        new AnonymousAuthenticationToken(
            "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);

    assertThrows(
        ResourceNotFoundException.class,
        () -> profileDatafetcher.queryProfile(username, dataFetchingEnvironment));
  }

  @Test
  void queryProfile_should_pass_current_user_when_authenticated() {
    User currentUser = new User("me@example.com", "meuser", "password", "bio", "image");
    TestingAuthenticationToken authentication =
        new TestingAuthenticationToken(currentUser, null, "ROLE_USER");
    SecurityContextHolder.getContext().setAuthentication(authentication);

    String username = "targetuser";
    when(dataFetchingEnvironment.getArgument("username")).thenReturn(username);

    ProfileData profileData =
        new ProfileData("target-id", "targetuser", "target bio", "target image", true);
    when(profileQueryService.findByUsername(eq("targetuser"), eq(currentUser)))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result = profileDatafetcher.queryProfile(username, dataFetchingEnvironment);

    assertNotNull(result);
    assertTrue(result.getProfile().getFollowing());
    verify(profileQueryService).findByUsername(eq("targetuser"), eq(currentUser));
  }
}
