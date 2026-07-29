package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

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
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileDatafetcherTest extends GraphqlTestBase {

  @Mock private ProfileQueryService profileQueryService;

  @InjectMocks private ProfileDatafetcher profileDatafetcher;

  private User user;
  private ProfileData profileData;

  @BeforeEach
  void setUp() {
    user = new User("john@jacob.com", "johnjacob", "123", "bio", "image");
    profileData = new ProfileData(user.getId(), user.getUsername(), "bio", "image", true);
  }

  @Test
  void should_get_profile_of_a_user() {
    login(user);
    when(environment.<User>getLocalContext()).thenReturn(user);
    when(profileQueryService.findByUsername(user.getUsername(), user))
        .thenReturn(Optional.of(profileData));

    Profile profile = profileDatafetcher.getUserProfile(environment);

    assertThat(profile.getUsername()).isEqualTo(user.getUsername());
    assertThat(profile.getBio()).isEqualTo("bio");
    assertThat(profile.getImage()).isEqualTo("image");
    assertThat(profile.getFollowing()).isTrue();
  }

  @Test
  void should_get_author_of_an_article() {
    logout();
    ArticleData articleData =
        new ArticleData(
            "id",
            "slug",
            "title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            null,
            profileData);
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put("slug", articleData);
    when(environment.<Map<String, ArticleData>>getLocalContext()).thenReturn(localContext);
    when(environment.getSource()).thenReturn(Article.newBuilder().slug("slug").build());
    when(profileQueryService.findByUsername(user.getUsername(), null))
        .thenReturn(Optional.of(profileData));

    assertThat(profileDatafetcher.getAuthor(environment).getUsername())
        .isEqualTo(user.getUsername());
  }

  @Test
  void should_get_author_of_a_comment() {
    logout();
    CommentData commentData =
        new CommentData(
            "comment-id", "body", "article-id", new DateTime(), new DateTime(), profileData);
    Map<String, CommentData> localContext = new HashMap<>();
    localContext.put("comment-id", commentData);
    when(environment.<Map<String, CommentData>>getLocalContext()).thenReturn(localContext);
    when(environment.getSource()).thenReturn(Comment.newBuilder().id("comment-id").build());
    when(profileQueryService.findByUsername(user.getUsername(), null))
        .thenReturn(Optional.of(profileData));

    assertThat(profileDatafetcher.getCommentAuthor(environment).getUsername())
        .isEqualTo(user.getUsername());
  }

  @Test
  void should_query_profile_by_username() {
    login(user);
    when(environment.<String>getArgument("username")).thenReturn(user.getUsername());
    when(profileQueryService.findByUsername(user.getUsername(), user))
        .thenReturn(Optional.of(profileData));

    ProfilePayload payload = profileDatafetcher.queryProfile(user.getUsername(), environment);

    assertThat(payload.getProfile().getUsername()).isEqualTo(user.getUsername());
  }

  @Test
  void should_fail_when_profile_not_found() {
    logout();
    when(environment.<String>getArgument("username")).thenReturn("unknown");
    when(profileQueryService.findByUsername("unknown", null)).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> profileDatafetcher.queryProfile("unknown", environment));
  }
}
