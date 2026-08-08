package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ProfileDatafetcherTest {

  @Mock private ProfileQueryService profileQueryService;
  @Mock private DataFetchingEnvironment dfe;

  private ProfileDatafetcher profileDatafetcher;

  @BeforeEach
  public void setUp() {
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private ProfileData profileData(String username) {
    return new ProfileData("user-id", username, "bio", "image", true);
  }

  @Test
  public void should_get_user_profile_from_local_context_for_anonymous_viewer() {
    User user = new User("a@b.com", "alice", "123", "", "");
    when(dfe.<User>getLocalContext()).thenReturn(user);
    when(profileQueryService.findByUsername("alice", null))
        .thenReturn(Optional.of(profileData("alice")));

    Profile profile = profileDatafetcher.getUserProfile(dfe);

    assertThat(profile.getUsername()).isEqualTo("alice");
    assertThat(profile.getBio()).isEqualTo("bio");
    assertThat(profile.getImage()).isEqualTo("image");
    assertThat(profile.getFollowing()).isTrue();
  }

  @Test
  public void should_throw_not_found_when_profile_missing() {
    User user = new User("a@b.com", "ghost", "123", "", "");
    when(dfe.<User>getLocalContext()).thenReturn(user);
    when(profileQueryService.findByUsername("ghost", null)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> profileDatafetcher.getUserProfile(dfe))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  public void should_get_article_author_profile_with_current_user() {
    User current = new User("c@b.com", "current", "123", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(current, null, Collections.emptyList()));

    ArticleData articleData = new ArticleData();
    articleData.setProfileData(profileData("author"));
    Map<String, ArticleData> map = new HashMap<>();
    map.put("a-slug", articleData);
    when(dfe.<Map<String, ArticleData>>getLocalContext()).thenReturn(map);
    when(dfe.<Article>getSource()).thenReturn(Article.newBuilder().slug("a-slug").build());
    when(profileQueryService.findByUsername(eq("author"), eq(current)))
        .thenReturn(Optional.of(profileData("author")));

    Profile profile = profileDatafetcher.getAuthor(dfe);

    assertThat(profile.getUsername()).isEqualTo("author");
    assertThat(profile.getFollowing()).isTrue();
  }

  @Test
  public void should_get_comment_author_profile() {
    CommentData commentData =
        new CommentData(
            "comment-id",
            "body",
            "article-id",
            new DateTime(1000L),
            new DateTime(1000L),
            profileData("commenter"));
    Map<String, CommentData> map = new HashMap<>();
    map.put("comment-id", commentData);
    when(dfe.<Map<String, CommentData>>getLocalContext()).thenReturn(map);
    when(dfe.<Comment>getSource()).thenReturn(Comment.newBuilder().id("comment-id").build());
    when(profileQueryService.findByUsername("commenter", null))
        .thenReturn(Optional.of(profileData("commenter")));

    Profile profile = profileDatafetcher.getCommentAuthor(dfe);

    assertThat(profile.getUsername()).isEqualTo("commenter");
  }

  @Test
  public void should_query_profile_by_username_argument() {
    when(dfe.<String>getArgument("username")).thenReturn("alice");
    when(profileQueryService.findByUsername("alice", null))
        .thenReturn(Optional.of(profileData("alice")));

    ProfilePayload payload = profileDatafetcher.queryProfile("alice", dfe);

    assertThat(payload.getProfile().getUsername()).isEqualTo("alice");
    assertThat(payload.getProfile().getBio()).isEqualTo("bio");
  }
}
