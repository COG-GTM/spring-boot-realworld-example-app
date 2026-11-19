package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
  private User user;

  @BeforeEach
  public void setUp() {
    profileQueryService = mock(ProfileQueryService.class);
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);
    dfe = mock(DataFetchingEnvironment.class);
    user = new User("a@b.com", "alice", "pw", "", "");
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

  private ProfileData bob(boolean following) {
    return new ProfileData("bob-id", "bob", "bob bio", "bob.png", following);
  }

  @Test
  public void getUserProfile_should_resolve_profile_of_local_context_user() {
    authenticate(user);
    when(dfe.getLocalContext()).thenReturn(user);
    when(profileQueryService.findByUsername("alice", user))
        .thenReturn(Optional.of(new ProfileData(user.getId(), "alice", "", "", false)));

    Profile profile = profileDatafetcher.getUserProfile(dfe);

    assertThat(profile.getUsername()).isEqualTo("alice");
    verify(profileQueryService).findByUsername("alice", user);
  }

  @Test
  public void getAuthor_should_resolve_author_from_article_data_map() {
    authenticate(user);
    ArticleData articleData =
        new ArticleData(
            "id",
            "my-slug",
            "t",
            "d",
            "b",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            bob(true));
    Map<String, ArticleData> map = new HashMap<>();
    map.put("my-slug", articleData);
    when(dfe.getLocalContext()).thenReturn(map);
    when(dfe.getSource()).thenReturn(Article.newBuilder().slug("my-slug").build());
    when(profileQueryService.findByUsername("bob", user)).thenReturn(Optional.of(bob(true)));

    Profile profile = profileDatafetcher.getAuthor(dfe);

    assertThat(profile.getUsername()).isEqualTo("bob");
    assertThat(profile.getBio()).isEqualTo("bob bio");
    assertThat(profile.getImage()).isEqualTo("bob.png");
    assertThat(profile.getFollowing()).isTrue();
  }

  @Test
  public void getCommentAuthor_should_resolve_author_from_comment_data_map() {
    authenticate(user);
    CommentData commentData =
        new CommentData("c1", "body", "a1", new DateTime(), new DateTime(), bob(false));
    Map<String, CommentData> map = new HashMap<>();
    map.put("c1", commentData);
    when(dfe.getLocalContext()).thenReturn(map);
    when(dfe.getSource()).thenReturn(Comment.newBuilder().id("c1").build());
    when(profileQueryService.findByUsername("bob", user)).thenReturn(Optional.of(bob(false)));

    Profile profile = profileDatafetcher.getCommentAuthor(dfe);

    assertThat(profile.getUsername()).isEqualTo("bob");
    assertThat(profile.getFollowing()).isFalse();
  }

  @Test
  public void queryProfile_should_return_profile_for_authenticated_user() {
    authenticate(user);
    when(dfe.getArgument("username")).thenReturn("bob");
    when(profileQueryService.findByUsername("bob", user)).thenReturn(Optional.of(bob(true)));

    ProfilePayload payload = profileDatafetcher.queryProfile("bob", dfe);

    assertThat(payload.getProfile().getUsername()).isEqualTo("bob");
    assertThat(payload.getProfile().getFollowing()).isTrue();
    verify(profileQueryService).findByUsername("bob", user);
  }

  @Test
  public void queryProfile_should_pass_null_current_user_when_anonymous() {
    anonymous();
    when(dfe.getArgument("username")).thenReturn("bob");
    when(profileQueryService.findByUsername("bob", null)).thenReturn(Optional.of(bob(false)));

    ProfilePayload payload = profileDatafetcher.queryProfile("bob", dfe);

    assertThat(payload.getProfile().getUsername()).isEqualTo("bob");
    assertThat(payload.getProfile().getFollowing()).isFalse();
    verify(profileQueryService).findByUsername("bob", null);
  }

  @Test
  public void queryProfile_should_throw_not_found_when_user_missing() {
    anonymous();
    when(dfe.getArgument("username")).thenReturn("ghost");
    when(profileQueryService.findByUsername(org.mockito.ArgumentMatchers.eq("ghost"), isNull()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> profileDatafetcher.queryProfile("ghost", dfe))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
