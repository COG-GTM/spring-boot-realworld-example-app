package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProfileDatafetcherTest {

  @Mock private ProfileQueryService profileQueryService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  @InjectMocks private ProfileDatafetcher profileDatafetcher;

  private final User currentUser = new User("jake@jake.jake", "jake", "123", "bio", "image");
  private final ProfileData profileData =
      new ProfileData("author-id", "john", "john's bio", "john's image", true);

  @AfterEach
  void tearDown() {
    SecurityContextHelper.clear();
  }

  @Test
  public void should_build_profile_from_user_local_context() {
    SecurityContextHelper.authenticate(currentUser);
    User target = new User("john@john.com", "john", "123", "john's bio", "john's image");
    when(dataFetchingEnvironment.<User>getLocalContext()).thenReturn(target);
    when(profileQueryService.findByUsername("john", currentUser))
        .thenReturn(Optional.of(profileData));

    Profile profile = profileDatafetcher.getUserProfile(dataFetchingEnvironment);

    assertEquals("john", profile.getUsername());
    assertEquals("john's bio", profile.getBio());
    assertEquals("john's image", profile.getImage());
    assertTrue(profile.getFollowing());
  }

  @Test
  public void should_resolve_article_author_from_local_context_map() {
    SecurityContextHelper.anonymous();
    ArticleData articleData = articleData("a-title");
    Map<String, ArticleData> localContext = Collections.singletonMap("a-title", articleData);
    when(dataFetchingEnvironment.<Map<String, ArticleData>>getLocalContext())
        .thenReturn(localContext);
    when(dataFetchingEnvironment.<Article>getSource())
        .thenReturn(Article.newBuilder().slug("a-title").build());
    when(profileQueryService.findByUsername("john", null)).thenReturn(Optional.of(profileData));

    Profile profile = profileDatafetcher.getAuthor(dataFetchingEnvironment);

    assertEquals("john", profile.getUsername());
  }

  @Test
  public void should_resolve_comment_author_from_local_context_map() {
    SecurityContextHelper.authenticate(currentUser);
    CommentData commentData =
        new CommentData(
            "comment-id",
            "comment body",
            "article-id",
            new DateTime(),
            new DateTime(),
            profileData);
    when(dataFetchingEnvironment.<Map<String, CommentData>>getLocalContext())
        .thenReturn(Collections.singletonMap("comment-id", commentData));
    when(dataFetchingEnvironment.<Comment>getSource())
        .thenReturn(Comment.newBuilder().id("comment-id").build());
    when(profileQueryService.findByUsername("john", currentUser))
        .thenReturn(Optional.of(profileData));

    Profile profile = profileDatafetcher.getCommentAuthor(dataFetchingEnvironment);

    assertEquals("john", profile.getUsername());
  }

  @Test
  public void should_query_profile_by_username_argument() {
    SecurityContextHelper.anonymous();
    when(dataFetchingEnvironment.<String>getArgument("username")).thenReturn("john");
    when(profileQueryService.findByUsername("john", null)).thenReturn(Optional.of(profileData));

    ProfilePayload payload = profileDatafetcher.queryProfile("john", dataFetchingEnvironment);

    assertEquals("john", payload.getProfile().getUsername());
    assertTrue(payload.getProfile().getFollowing());
  }

  @Test
  public void should_expose_not_following_profile() {
    SecurityContextHelper.anonymous();
    when(dataFetchingEnvironment.<String>getArgument("username")).thenReturn("john");
    when(profileQueryService.findByUsername("john", null))
        .thenReturn(Optional.of(new ProfileData("author-id", "john", null, null, false)));

    ProfilePayload payload = profileDatafetcher.queryProfile("john", dataFetchingEnvironment);

    assertFalse(payload.getProfile().getFollowing());
  }

  @Test
  public void should_throw_when_profile_is_not_found() {
    SecurityContextHelper.anonymous();
    when(dataFetchingEnvironment.<String>getArgument("username")).thenReturn("missing");
    when(profileQueryService.findByUsername("missing", null)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> profileDatafetcher.queryProfile("missing", dataFetchingEnvironment));
  }

  private ArticleData articleData(String slug) {
    DateTime now = new DateTime();
    return new ArticleData(
        "article-id",
        slug,
        "a title",
        "desc",
        "body",
        false,
        0,
        now,
        now,
        Collections.singletonList("java"),
        profileData);
  }
}
