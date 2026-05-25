package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ProfileDatafetcherTest {

  @Mock private ProfileQueryService profileQueryService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private ProfileDatafetcher profileDatafetcher;
  private User user;
  private ProfileData profileData;

  @BeforeEach
  void setUp() {
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);
    user = new User("test@test.com", "testuser", "password", "my bio", "http://image.url");
    profileData =
        new ProfileData(
            user.getId(), user.getUsername(), user.getBio(), user.getImage(), true);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
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

  // --- getUserProfile tests ---

  @Test
  void getUserProfile_returnsProfile() {
    setAuthenticatedUser(user);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);
    when(profileQueryService.findByUsername("testuser", user))
        .thenReturn(Optional.of(profileData));

    Profile result = profileDatafetcher.getUserProfile(dataFetchingEnvironment);

    assertNotNull(result);
    assertEquals("testuser", result.getUsername());
    assertEquals("my bio", result.getBio());
    assertEquals("http://image.url", result.getImage());
    assertTrue(result.getFollowing());
  }

  @Test
  void getUserProfile_profileNotFound_throwsResourceNotFound() {
    setAnonymousUser();
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);
    when(profileQueryService.findByUsername("testuser", null)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> profileDatafetcher.getUserProfile(dataFetchingEnvironment));
  }

  // --- getAuthor tests ---

  @Test
  void getAuthor_returnsProfileForArticleAuthor() {
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
    Map<String, ArticleData> map = new HashMap<>();
    map.put("test-slug", articleData);

    when(dataFetchingEnvironment.getLocalContext()).thenReturn(map);
    when(dataFetchingEnvironment.getSource()).thenReturn(article);
    when(profileQueryService.findByUsername("testuser", user))
        .thenReturn(Optional.of(profileData));

    Profile result = profileDatafetcher.getAuthor(dataFetchingEnvironment);

    assertNotNull(result);
    assertEquals("testuser", result.getUsername());
  }

  @Test
  void getAuthor_authorNotFound_throwsResourceNotFound() {
    setAnonymousUser();
    ProfileData unknownProfile = new ProfileData("id", "unknown", "", "", false);
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
            unknownProfile);
    Map<String, ArticleData> map = new HashMap<>();
    map.put("test-slug", articleData);

    when(dataFetchingEnvironment.getLocalContext()).thenReturn(map);
    when(dataFetchingEnvironment.getSource()).thenReturn(article);
    when(profileQueryService.findByUsername("unknown", null)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> profileDatafetcher.getAuthor(dataFetchingEnvironment));
  }

  // --- getCommentAuthor tests ---

  @Test
  void getCommentAuthor_returnsProfileForCommentAuthor() {
    setAuthenticatedUser(user);
    Comment comment = Comment.newBuilder().id("c1").build();
    CommentData commentData =
        new CommentData("c1", "body", "art-id", new DateTime(), new DateTime(), profileData);
    Map<String, CommentData> map = new HashMap<>();
    map.put("c1", commentData);

    when(dataFetchingEnvironment.getSource()).thenReturn(comment);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(map);
    when(profileQueryService.findByUsername("testuser", user))
        .thenReturn(Optional.of(profileData));

    Profile result = profileDatafetcher.getCommentAuthor(dataFetchingEnvironment);

    assertNotNull(result);
    assertEquals("testuser", result.getUsername());
  }

  @Test
  void getCommentAuthor_authorNotFound_throwsResourceNotFound() {
    setAnonymousUser();
    ProfileData unknownProfile = new ProfileData("id", "ghost", "", "", false);
    Comment comment = Comment.newBuilder().id("c1").build();
    CommentData commentData =
        new CommentData("c1", "body", "art-id", new DateTime(), new DateTime(), unknownProfile);
    Map<String, CommentData> map = new HashMap<>();
    map.put("c1", commentData);

    when(dataFetchingEnvironment.getSource()).thenReturn(comment);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(map);
    when(profileQueryService.findByUsername("ghost", null)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> profileDatafetcher.getCommentAuthor(dataFetchingEnvironment));
  }

  // --- queryProfile (Query.profile) tests ---

  @Test
  void queryProfile_returnsProfilePayload() {
    setAuthenticatedUser(user);
    when(dataFetchingEnvironment.getArgument("username")).thenReturn("testuser");
    when(profileQueryService.findByUsername("testuser", user))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result =
        profileDatafetcher.queryProfile("testuser", dataFetchingEnvironment);

    assertNotNull(result);
    assertNotNull(result.getProfile());
    assertEquals("testuser", result.getProfile().getUsername());
    assertEquals("my bio", result.getProfile().getBio());
    assertEquals("http://image.url", result.getProfile().getImage());
    assertTrue(result.getProfile().getFollowing());
  }

  @Test
  void queryProfile_userNotFound_throwsResourceNotFound() {
    setAnonymousUser();
    when(dataFetchingEnvironment.getArgument("username")).thenReturn("nonexistent");
    when(profileQueryService.findByUsername("nonexistent", null)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> profileDatafetcher.queryProfile("nonexistent", dataFetchingEnvironment));
  }

  @Test
  void queryProfile_anonymous_returnsProfileWithoutFollowing() {
    setAnonymousUser();
    ProfileData notFollowing =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    when(dataFetchingEnvironment.getArgument("username")).thenReturn("testuser");
    when(profileQueryService.findByUsername("testuser", null))
        .thenReturn(Optional.of(notFollowing));

    ProfilePayload result =
        profileDatafetcher.queryProfile("testuser", dataFetchingEnvironment);

    assertNotNull(result);
    assertFalse(result.getProfile().getFollowing());
  }
}
