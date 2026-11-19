package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.joda.time.DateTime;
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
  private DataFetchingEnvironment dfe;
  private User current;
  private ProfileData bobProfile;

  @BeforeEach
  public void setUp() {
    profileQueryService = mock(ProfileQueryService.class);
    dfe = mock(DataFetchingEnvironment.class);
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);
    current = new User("alice@example.com", "alice", "pw", "", "");
    bobProfile = new ProfileData("bob-id", "bob", "bob bio", "bob.png", true);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  private void assertBobProfile(Profile profile) {
    assertEquals("bob", profile.getUsername());
    assertEquals("bob bio", profile.getBio());
    assertEquals("bob.png", profile.getImage());
    assertTrue(profile.getFollowing());
  }

  @Test
  public void should_resolve_user_profile_from_local_context_user() {
    authenticate(current);
    User bob = new User("bob@example.com", "bob", "pw", "", "");
    when(dfe.getLocalContext()).thenReturn(bob);
    when(profileQueryService.findByUsername("bob", current)).thenReturn(Optional.of(bobProfile));

    Profile profile = profileDatafetcher.getUserProfile(dfe);

    assertBobProfile(profile);
    verify(profileQueryService).findByUsername("bob", current);
  }

  @Test
  public void should_resolve_article_author_from_local_context_map() {
    authenticate(current);
    ArticleData articleData =
        new ArticleData(
            "a1",
            "my-slug",
            "t",
            "d",
            "b",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            bobProfile);
    Map<String, ArticleData> map = new HashMap<>();
    map.put("my-slug", articleData);
    when(dfe.getLocalContext()).thenReturn(map);
    when(dfe.getSource()).thenReturn(Article.newBuilder().slug("my-slug").build());
    when(profileQueryService.findByUsername("bob", current)).thenReturn(Optional.of(bobProfile));

    Profile profile = profileDatafetcher.getAuthor(dfe);

    assertBobProfile(profile);
  }

  @Test
  public void should_resolve_comment_author_from_local_context_map() {
    anonymous();
    CommentData commentData =
        new CommentData("c1", "body", "a1", new DateTime(), new DateTime(), bobProfile);
    Map<String, CommentData> map = new HashMap<>();
    map.put("c1", commentData);
    when(dfe.getLocalContext()).thenReturn(map);
    when(dfe.getSource()).thenReturn(Comment.newBuilder().id("c1").build());
    when(profileQueryService.findByUsername(org.mockito.ArgumentMatchers.eq("bob"), isNull()))
        .thenReturn(Optional.of(bobProfile));

    Profile profile = profileDatafetcher.getCommentAuthor(dfe);

    assertBobProfile(profile);
  }

  @Test
  public void should_query_profile_by_username_argument_when_authenticated() {
    authenticate(current);
    when(dfe.getArgument("username")).thenReturn("bob");
    when(profileQueryService.findByUsername("bob", current)).thenReturn(Optional.of(bobProfile));

    ProfilePayload payload = profileDatafetcher.queryProfile("bob", dfe);

    assertBobProfile(payload.getProfile());
    verify(profileQueryService).findByUsername("bob", current);
  }

  @Test
  public void should_query_profile_as_anonymous_with_null_current_user() {
    anonymous();
    ProfileData notFollowing = new ProfileData("bob-id", "bob", "bio", "img", false);
    when(dfe.getArgument("username")).thenReturn("bob");
    when(profileQueryService.findByUsername(org.mockito.ArgumentMatchers.eq("bob"), isNull()))
        .thenReturn(Optional.of(notFollowing));

    ProfilePayload payload = profileDatafetcher.queryProfile("bob", dfe);

    assertEquals("bob", payload.getProfile().getUsername());
    assertFalse(payload.getProfile().getFollowing());
    verify(profileQueryService).findByUsername(org.mockito.ArgumentMatchers.eq("bob"), isNull());
  }

  @Test
  public void should_throw_not_found_for_unknown_username() {
    authenticate(current);
    when(dfe.getArgument("username")).thenReturn("ghost");
    when(profileQueryService.findByUsername("ghost", current)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> profileDatafetcher.queryProfile("ghost", dfe));
  }

  @Test
  public void should_use_environment_argument_rather_than_method_parameter() {
    authenticate(current);
    when(dfe.getArgument("username")).thenReturn("bob");
    when(profileQueryService.findByUsername("bob", current)).thenReturn(Optional.of(bobProfile));

    ProfilePayload payload = profileDatafetcher.queryProfile("ignored", dfe);

    assertEquals("bob", payload.getProfile().getUsername());
  }
}
