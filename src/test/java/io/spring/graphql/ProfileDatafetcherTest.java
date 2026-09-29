package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class ProfileDatafetcherTest {

  private ProfileQueryService profileQueryService;
  private ProfileDatafetcher profileDatafetcher;

  private User currentUser;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    profileQueryService = mock(ProfileQueryService.class);
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);

    currentUser = new User("current@example.com", "current", "pass", "", "");
    profileData = new ProfileData("profile-id", "targetUser", "the bio", "the-image.png", true);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  private void assertMatchesFixture(Profile profile) {
    assertEquals(profileData.getUsername(), profile.getUsername());
    assertEquals(profileData.getBio(), profile.getBio());
    assertEquals(profileData.getImage(), profile.getImage());
    assertEquals(profileData.isFollowing(), profile.getFollowing());
  }

  @Test
  public void should_get_user_profile_from_local_context() {
    authenticate(currentUser);
    when(profileQueryService.findByUsername(eq("targetUser"), eq(currentUser)))
        .thenReturn(Optional.of(profileData));

    User source = new User("target@example.com", "targetUser", "pass", "", "");
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<User>getLocalContext()).thenReturn(source);

    Profile profile = profileDatafetcher.getUserProfile(dfe);

    assertMatchesFixture(profile);
    verify(profileQueryService).findByUsername(eq("targetUser"), eq(currentUser));
  }

  @Test
  public void should_get_user_profile_when_unauthenticated() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    when(profileQueryService.findByUsername(eq("targetUser"), isNull()))
        .thenReturn(Optional.of(profileData));

    User source = new User("target@example.com", "targetUser", "pass", "", "");
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<User>getLocalContext()).thenReturn(source);

    Profile profile = profileDatafetcher.getUserProfile(dfe);

    assertMatchesFixture(profile);
  }

  @Test
  public void should_throw_when_profile_not_found() {
    authenticate(currentUser);
    when(profileQueryService.findByUsername(any(), any())).thenReturn(Optional.empty());

    User source = new User("target@example.com", "targetUser", "pass", "", "");
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<User>getLocalContext()).thenReturn(source);

    assertThrows(ResourceNotFoundException.class, () -> profileDatafetcher.getUserProfile(dfe));
  }

  @Test
  public void should_get_author_from_article_source_and_local_context() {
    authenticate(currentUser);
    when(profileQueryService.findByUsername(eq("targetUser"), eq(currentUser)))
        .thenReturn(Optional.of(profileData));

    ArticleData articleData = new ArticleData();
    articleData.setSlug("a-slug");
    articleData.setProfileData(profileData);
    Map<String, ArticleData> map = new HashMap<>();
    map.put("a-slug", articleData);

    Article source = Article.newBuilder().slug("a-slug").build();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<Map<String, ArticleData>>getLocalContext()).thenReturn(map);
    when(dfe.<Article>getSource()).thenReturn(source);

    Profile profile = profileDatafetcher.getAuthor(dfe);

    assertMatchesFixture(profile);
    verify(profileQueryService).findByUsername(eq("targetUser"), eq(currentUser));
  }

  @Test
  public void should_get_comment_author_from_comment_source_and_local_context() {
    authenticate(currentUser);
    when(profileQueryService.findByUsername(eq("targetUser"), eq(currentUser)))
        .thenReturn(Optional.of(profileData));

    CommentData commentData = new CommentData();
    commentData.setId("comment-id");
    commentData.setProfileData(profileData);
    Map<String, CommentData> map = new HashMap<>();
    map.put("comment-id", commentData);

    Comment source = Comment.newBuilder().id("comment-id").build();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<Map<String, CommentData>>getLocalContext()).thenReturn(map);
    when(dfe.<Comment>getSource()).thenReturn(source);

    Profile profile = profileDatafetcher.getCommentAuthor(dfe);

    assertMatchesFixture(profile);
    verify(profileQueryService).findByUsername(eq("targetUser"), eq(currentUser));
  }

  @Test
  public void should_query_profile_by_argument() {
    authenticate(currentUser);
    when(profileQueryService.findByUsername(eq("targetUser"), eq(currentUser)))
        .thenReturn(Optional.of(profileData));

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<String>getArgument("username")).thenReturn("targetUser");

    ProfilePayload payload = profileDatafetcher.queryProfile("targetUser", dfe);

    assertNotNull(payload.getProfile());
    assertMatchesFixture(payload.getProfile());
    verify(profileQueryService).findByUsername(eq("targetUser"), eq(currentUser));
  }
}
