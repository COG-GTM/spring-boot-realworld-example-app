package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import io.spring.TestHelper;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.Profile;
import io.spring.graphql.types.ProfilePayload;
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileDatafetcherTest extends GraphQLTestBase {

  @Mock private ProfileQueryService profileQueryService;

  @InjectMocks private ProfileDatafetcher profileDatafetcher;

  private ProfileData profileData() {
    return new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), true);
  }

  @Test
  void should_get_user_profile_from_local_context() {
    when(profileQueryService.findByUsername(eq(user.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileData()));

    Profile profile = profileDatafetcher.getUserProfile(dfe(null, user));

    assertThat(profile.getUsername()).isEqualTo(user.getUsername());
    assertThat(profile.getBio()).isEqualTo(user.getBio());
    assertThat(profile.getImage()).isEqualTo(user.getImage());
    assertThat(profile.getFollowing()).isTrue();
  }

  @Test
  void should_get_article_author() {
    ArticleData articleData = TestHelper.articleDataFixture("test", user);
    when(profileQueryService.findByUsername(eq(user.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileData()));

    Profile profile =
        profileDatafetcher.getAuthor(
            dfe(
                Article.newBuilder().slug(articleData.getSlug()).build(),
                Collections.singletonMap(articleData.getSlug(), articleData)));

    assertThat(profile.getUsername()).isEqualTo(user.getUsername());
  }

  @Test
  void should_get_comment_author() {
    CommentData commentData =
        new CommentData(
            "comment-id", "body", "article-id", new DateTime(), new DateTime(), profileData());
    when(profileQueryService.findByUsername(eq(user.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileData()));

    Profile profile =
        profileDatafetcher.getCommentAuthor(
            dfe(
                Comment.newBuilder().id(commentData.getId()).build(),
                Collections.singletonMap(commentData.getId(), commentData)));

    assertThat(profile.getUsername()).isEqualTo(user.getUsername());
  }

  @Test
  void should_query_profile_by_username_argument() {
    when(profileQueryService.findByUsername(eq(user.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileData()));

    ProfilePayload payload =
        profileDatafetcher.queryProfile(
            user.getUsername(),
            dfe(null, null, Collections.singletonMap("username", user.getUsername())));

    assertThat(payload.getProfile().getUsername()).isEqualTo(user.getUsername());
  }

  @Test
  void should_query_profile_for_anonymous_user() {
    anonymous();
    when(profileQueryService.findByUsername(eq(user.getUsername()), isNull()))
        .thenReturn(Optional.of(profileData()));

    ProfilePayload payload =
        profileDatafetcher.queryProfile(
            user.getUsername(),
            dfe(null, null, Collections.singletonMap("username", user.getUsername())));

    assertThat(payload.getProfile().getUsername()).isEqualTo(user.getUsername());
  }

  @Test
  void should_fail_when_profile_not_found() {
    when(profileQueryService.findByUsername(eq("unknown"), eq(user))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(
            () ->
                profileDatafetcher.queryProfile(
                    "unknown", dfe(null, null, Collections.singletonMap("username", "unknown"))));
  }
}
