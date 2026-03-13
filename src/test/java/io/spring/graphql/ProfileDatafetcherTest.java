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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ProfileDatafetcherTest {

  @Mock private ProfileQueryService profileQueryService;

  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  @InjectMocks private ProfileDatafetcher profileDatafetcher;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getUserProfile_success() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.png");
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);

    User currentUser = new User("me@example.com", "me", "password", "", "");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(currentUser, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    ProfileData profileData = new ProfileData(user.getId(), "testuser", "bio", "image.png", true);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    Profile result = profileDatafetcher.getUserProfile(dataFetchingEnvironment);

    assertNotNull(result);
    assertEquals("testuser", result.getUsername());
    assertEquals("bio", result.getBio());
    assertEquals("image.png", result.getImage());
    assertTrue(result.getFollowing());
  }

  @Test
  void getUserProfile_notFound() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.png");
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);

    User currentUser = new User("me@example.com", "me", "password", "", "");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(currentUser, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    when(profileQueryService.findByUsername(eq("testuser"), any())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> profileDatafetcher.getUserProfile(dataFetchingEnvironment));
  }

  @Test
  void getUserProfile_anonymousUser() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.png");
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);

    AnonymousAuthenticationToken anonAuth =
        new AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            java.util.Collections.singletonList(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonAuth);

    ProfileData profileData =
        new ProfileData(user.getId(), "testuser", "bio", "image.png", false);
    when(profileQueryService.findByUsername(eq("testuser"), eq(null)))
        .thenReturn(Optional.of(profileData));

    Profile result = profileDatafetcher.getUserProfile(dataFetchingEnvironment);

    assertNotNull(result);
    assertEquals("testuser", result.getUsername());
    assertFalse(result.getFollowing());
  }

  @Test
  void getAuthor_success() {
    User currentUser = new User("me@example.com", "me", "password", "", "");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(currentUser, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    ProfileData authorProfile = new ProfileData("authorId", "authoruser", "bio", "img.png", false);
    ArticleData articleData = new ArticleData();
    articleData.setSlug("test-article");
    articleData.setProfileData(authorProfile);

    Map<String, ArticleData> map = new HashMap<>();
    map.put("test-article", articleData);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(map);

    Article article = Article.newBuilder().slug("test-article").build();
    when(dataFetchingEnvironment.getSource()).thenReturn(article);

    ProfileData profileData =
        new ProfileData("authorId", "authoruser", "bio", "img.png", false);
    when(profileQueryService.findByUsername(eq("authoruser"), any()))
        .thenReturn(Optional.of(profileData));

    Profile result = profileDatafetcher.getAuthor(dataFetchingEnvironment);

    assertNotNull(result);
    assertEquals("authoruser", result.getUsername());
    assertEquals("bio", result.getBio());
    assertEquals("img.png", result.getImage());
  }

  @Test
  void getCommentAuthor_success() {
    User currentUser = new User("me@example.com", "me", "password", "", "");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(currentUser, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    ProfileData commentAuthorProfile =
        new ProfileData("commenterId", "commenter", "bio", "img.png", true);
    CommentData commentData = new CommentData();
    commentData.setId("comment-1");
    commentData.setProfileData(commentAuthorProfile);

    Map<String, CommentData> map = new HashMap<>();
    map.put("comment-1", commentData);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(map);

    Comment comment = Comment.newBuilder().id("comment-1").build();
    when(dataFetchingEnvironment.getSource()).thenReturn(comment);

    ProfileData profileData =
        new ProfileData("commenterId", "commenter", "bio", "img.png", true);
    when(profileQueryService.findByUsername(eq("commenter"), any()))
        .thenReturn(Optional.of(profileData));

    Profile result = profileDatafetcher.getCommentAuthor(dataFetchingEnvironment);

    assertNotNull(result);
    assertEquals("commenter", result.getUsername());
    assertTrue(result.getFollowing());
  }

  @Test
  void queryProfile_success() {
    when(dataFetchingEnvironment.getArgument("username")).thenReturn("targetuser");

    User currentUser = new User("me@example.com", "me", "password", "", "");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(currentUser, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    ProfileData profileData =
        new ProfileData("targetId", "targetuser", "target bio", "target.png", false);
    when(profileQueryService.findByUsername(eq("targetuser"), any()))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result =
        profileDatafetcher.queryProfile("targetuser", dataFetchingEnvironment);

    assertNotNull(result);
    assertNotNull(result.getProfile());
    assertEquals("targetuser", result.getProfile().getUsername());
    assertEquals("target bio", result.getProfile().getBio());
    assertEquals("target.png", result.getProfile().getImage());
  }

  @Test
  void queryProfile_notFound() {
    when(dataFetchingEnvironment.getArgument("username")).thenReturn("nonexistent");

    User currentUser = new User("me@example.com", "me", "password", "", "");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(currentUser, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    when(profileQueryService.findByUsername(eq("nonexistent"), any()))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> profileDatafetcher.queryProfile("nonexistent", dataFetchingEnvironment));
  }

  @Test
  void queryProfile_anonymousViewer() {
    when(dataFetchingEnvironment.getArgument("username")).thenReturn("targetuser");

    AnonymousAuthenticationToken anonAuth =
        new AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            java.util.Collections.singletonList(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonAuth);

    ProfileData profileData =
        new ProfileData("targetId", "targetuser", "target bio", "target.png", false);
    when(profileQueryService.findByUsername(eq("targetuser"), eq(null)))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result =
        profileDatafetcher.queryProfile("targetuser", dataFetchingEnvironment);

    assertNotNull(result);
    assertNotNull(result.getProfile());
    assertEquals("targetuser", result.getProfile().getUsername());
    assertFalse(result.getProfile().getFollowing());
  }
}
