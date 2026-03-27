package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ProfileDatafetcherTest {

  @Mock private ProfileQueryService profileQueryService;

  @InjectMocks private ProfileDatafetcher profileDatafetcher;

  private User user;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.clearContext();
    user = new User("test@example.com", "testuser", "password", "bio", "image");
    profileData =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
  }

  @org.junit.jupiter.api.AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_get_user_profile() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(user);
    when(profileQueryService.findByUsername(eq("testuser"), eq(user)))
        .thenReturn(Optional.of(profileData));

    Profile result = profileDatafetcher.getUserProfile(dfe);

    assertNotNull(result);
    assertEquals("testuser", result.getUsername());
    assertEquals("bio", result.getBio());
    assertEquals("image", result.getImage());
  }

  @Test
  public void should_get_article_author() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    Article article = Article.newBuilder().slug("test-slug").build();
    when(dfe.getSource()).thenReturn(article);

    ArticleData articleData =
        new ArticleData(
            "article-id",
            "test-slug",
            "Test Title",
            "Test Description",
            "Test Body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Arrays.asList("tag1", "tag2"),
            profileData);

    Map<String, ArticleData> articleMap = new HashMap<>();
    articleMap.put("test-slug", articleData);
    when(dfe.getLocalContext()).thenReturn(articleMap);

    when(profileQueryService.findByUsername(eq("testuser"), eq(user)))
        .thenReturn(Optional.of(profileData));

    Profile result = profileDatafetcher.getAuthor(dfe);

    assertNotNull(result);
    assertEquals("testuser", result.getUsername());
  }

  @Test
  public void should_get_comment_author() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    Comment comment = Comment.newBuilder().id("comment-id").build();
    when(dfe.getSource()).thenReturn(comment);

    CommentData commentData =
        new CommentData(
            "comment-id",
            "Test comment body",
            "article-id",
            new DateTime(),
            new DateTime(),
            profileData);

    Map<String, CommentData> commentMap = new HashMap<>();
    commentMap.put("comment-id", commentData);
    when(dfe.getLocalContext()).thenReturn(commentMap);

    when(profileQueryService.findByUsername(eq("testuser"), eq(user)))
        .thenReturn(Optional.of(profileData));

    Profile result = profileDatafetcher.getCommentAuthor(dfe);

    assertNotNull(result);
    assertEquals("testuser", result.getUsername());
  }

  @Test
  public void should_query_profile_by_username() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getArgument("username")).thenReturn("testuser");
    when(profileQueryService.findByUsername(eq("testuser"), eq(user)))
        .thenReturn(Optional.of(profileData));

    ProfilePayload result = profileDatafetcher.queryProfile("testuser", dfe);

    assertNotNull(result);
    assertNotNull(result.getProfile());
    assertEquals("testuser", result.getProfile().getUsername());
  }

  @Test
  public void should_fail_to_query_profile_not_found() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getArgument(eq("username"))).thenReturn("nonexistent");
    when(profileQueryService.findByUsername(eq("nonexistent"), eq(user)))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> profileDatafetcher.queryProfile("nonexistent", dfe));
  }
}
