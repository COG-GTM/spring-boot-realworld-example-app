package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProfileDatafetcherTest {

  @Mock private ProfileQueryService profileQueryService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private ProfileDatafetcher profileDatafetcher;
  private User user;
  private ProfileData profileData;

  @BeforeEach
  void setUp() {
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    profileData =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
  }

  @Test
  void getUserProfile_returnsProfile() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);
      when(profileQueryService.findByUsername(user.getUsername(), user))
          .thenReturn(Optional.of(profileData));

      Profile result = profileDatafetcher.getUserProfile(dataFetchingEnvironment);

      assertNotNull(result);
      assertEquals(profileData.getUsername(), result.getUsername());
      assertEquals(profileData.getBio(), result.getBio());
      assertEquals(profileData.getImage(), result.getImage());
      assertEquals(profileData.isFollowing(), result.getFollowing());
    }
  }

  @Test
  void getUserProfile_profileNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);
      when(profileQueryService.findByUsername(user.getUsername(), user))
          .thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> profileDatafetcher.getUserProfile(dataFetchingEnvironment));
    }
  }

  @Test
  void getAuthor_returnsProfile() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      DateTime now = DateTime.now();
      ArticleData articleData =
          new ArticleData(
              "article-id",
              "test-slug",
              "Test Title",
              "Test Description",
              "Test Body",
              false,
              0,
              now,
              now,
              Arrays.asList("tag1"),
              profileData);

      Map<String, ArticleData> articleMap = new HashMap<>();
      articleMap.put("test-slug", articleData);
      when(dataFetchingEnvironment.getLocalContext()).thenReturn(articleMap);

      Article article = Article.newBuilder().slug("test-slug").build();
      when(dataFetchingEnvironment.getSource()).thenReturn(article);
      when(profileQueryService.findByUsername(profileData.getUsername(), user))
          .thenReturn(Optional.of(profileData));

      Profile result = profileDatafetcher.getAuthor(dataFetchingEnvironment);

      assertNotNull(result);
      assertEquals(profileData.getUsername(), result.getUsername());
    }
  }

  @Test
  void getAuthor_profileNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      DateTime now = DateTime.now();
      ArticleData articleData =
          new ArticleData(
              "article-id",
              "test-slug",
              "Test Title",
              "Test Description",
              "Test Body",
              false,
              0,
              now,
              now,
              Arrays.asList("tag1"),
              profileData);

      Map<String, ArticleData> articleMap = new HashMap<>();
      articleMap.put("test-slug", articleData);
      when(dataFetchingEnvironment.getLocalContext()).thenReturn(articleMap);

      Article article = Article.newBuilder().slug("test-slug").build();
      when(dataFetchingEnvironment.getSource()).thenReturn(article);
      when(profileQueryService.findByUsername(profileData.getUsername(), user))
          .thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> profileDatafetcher.getAuthor(dataFetchingEnvironment));
    }
  }

  @Test
  void getCommentAuthor_returnsProfile() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      DateTime now = DateTime.now();
      CommentData commentData =
          new CommentData("comment-id", "comment body", "article-id", now, now, profileData);

      Map<String, CommentData> commentMap = new HashMap<>();
      commentMap.put("comment-id", commentData);
      when(dataFetchingEnvironment.getLocalContext()).thenReturn(commentMap);

      Comment comment = Comment.newBuilder().id("comment-id").build();
      when(dataFetchingEnvironment.getSource()).thenReturn(comment);
      when(profileQueryService.findByUsername(profileData.getUsername(), user))
          .thenReturn(Optional.of(profileData));

      Profile result = profileDatafetcher.getCommentAuthor(dataFetchingEnvironment);

      assertNotNull(result);
      assertEquals(profileData.getUsername(), result.getUsername());
    }
  }

  @Test
  void getCommentAuthor_profileNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      DateTime now = DateTime.now();
      CommentData commentData =
          new CommentData("comment-id", "comment body", "article-id", now, now, profileData);

      Map<String, CommentData> commentMap = new HashMap<>();
      commentMap.put("comment-id", commentData);
      when(dataFetchingEnvironment.getLocalContext()).thenReturn(commentMap);

      Comment comment = Comment.newBuilder().id("comment-id").build();
      when(dataFetchingEnvironment.getSource()).thenReturn(comment);
      when(profileQueryService.findByUsername(profileData.getUsername(), user))
          .thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> profileDatafetcher.getCommentAuthor(dataFetchingEnvironment));
    }
  }

  @Test
  void queryProfile_returnsProfilePayload() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(dataFetchingEnvironment.getArgument("username")).thenReturn("testuser");
      when(profileQueryService.findByUsername("testuser", user))
          .thenReturn(Optional.of(profileData));

      ProfilePayload result =
          profileDatafetcher.queryProfile("testuser", dataFetchingEnvironment);

      assertNotNull(result);
      assertNotNull(result.getProfile());
      assertEquals(profileData.getUsername(), result.getProfile().getUsername());
    }
  }

  @Test
  void queryProfile_profileNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(dataFetchingEnvironment.getArgument("username")).thenReturn("nonexistent");
      when(profileQueryService.findByUsername("nonexistent", user)).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> profileDatafetcher.queryProfile("nonexistent", dataFetchingEnvironment));
    }
  }

  @Test
  void queryProfile_withoutAuthenticatedUser_returnsProfilePayload() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      when(dataFetchingEnvironment.getArgument("username")).thenReturn("testuser");
      when(profileQueryService.findByUsername("testuser", null))
          .thenReturn(Optional.of(profileData));

      ProfilePayload result =
          profileDatafetcher.queryProfile("testuser", dataFetchingEnvironment);

      assertNotNull(result);
      assertNotNull(result.getProfile());
    }
  }

  @Test
  void getUserProfile_withFollowingTrue_returnsProfileWithFollowing() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      ProfileData followingProfile =
          new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), true);

      when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);
      when(profileQueryService.findByUsername(user.getUsername(), user))
          .thenReturn(Optional.of(followingProfile));

      Profile result = profileDatafetcher.getUserProfile(dataFetchingEnvironment);

      assertNotNull(result);
      assertTrue(result.getFollowing());
    }
  }

  @Test
  void getAuthor_withoutAuthenticatedUser_returnsProfile() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      DateTime now = DateTime.now();
      ArticleData articleData =
          new ArticleData(
              "article-id",
              "test-slug",
              "Test Title",
              "Test Description",
              "Test Body",
              false,
              0,
              now,
              now,
              Arrays.asList("tag1"),
              profileData);

      Map<String, ArticleData> articleMap = new HashMap<>();
      articleMap.put("test-slug", articleData);
      when(dataFetchingEnvironment.getLocalContext()).thenReturn(articleMap);

      Article article = Article.newBuilder().slug("test-slug").build();
      when(dataFetchingEnvironment.getSource()).thenReturn(article);
      when(profileQueryService.findByUsername(profileData.getUsername(), null))
          .thenReturn(Optional.of(profileData));

      Profile result = profileDatafetcher.getAuthor(dataFetchingEnvironment);

      assertNotNull(result);
      assertEquals(profileData.getUsername(), result.getUsername());
    }
  }
}
