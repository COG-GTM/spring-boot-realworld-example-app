package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ProfileDatafetcherTest {

  @Mock private ProfileQueryService profileQueryService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private ProfileDatafetcher profileDatafetcher;

  private final ProfileData profileData =
      new ProfileData("id-1", "jane", "jane bio", "jane-image", true);

  @BeforeEach
  void setUp() {
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_get_user_profile_from_local_context_user() {
    User user = new User("jane@example.com", "jane", "123", "bio", "image");
    when(dataFetchingEnvironment.<User>getLocalContext()).thenReturn(user);
    when(profileQueryService.findByUsername(eq("jane"), any()))
        .thenReturn(Optional.of(profileData));

    Profile profile = profileDatafetcher.getUserProfile(dataFetchingEnvironment);

    assertThat(profile.getUsername()).isEqualTo("jane");
    assertThat(profile.getBio()).isEqualTo("jane bio");
    assertThat(profile.getImage()).isEqualTo("jane-image");
    assertThat(profile.getFollowing()).isTrue();
  }

  @Test
  void should_get_author_from_article_source_and_local_context_map() {
    Article article = Article.newBuilder().slug("the-slug").build();
    ArticleData articleData =
        new ArticleData(
            "article-1",
            "the-slug",
            "title",
            "desc",
            "body",
            false,
            0,
            null,
            null,
            Collections.emptyList(),
            profileData);
    when(dataFetchingEnvironment.<java.util.Map<String, ArticleData>>getLocalContext())
        .thenReturn(Collections.singletonMap("the-slug", articleData));
    when(dataFetchingEnvironment.<Article>getSource()).thenReturn(article);
    when(profileQueryService.findByUsername(eq("jane"), any()))
        .thenReturn(Optional.of(profileData));

    Profile profile = profileDatafetcher.getAuthor(dataFetchingEnvironment);

    assertThat(profile.getUsername()).isEqualTo("jane");
  }

  @Test
  void should_get_comment_author_from_comment_source_and_local_context_map() {
    Comment comment = Comment.newBuilder().id("comment-1").build();
    CommentData commentData =
        new CommentData("comment-1", "body", "article-1", null, null, profileData);
    when(dataFetchingEnvironment.<java.util.Map<String, CommentData>>getLocalContext())
        .thenReturn(Collections.singletonMap("comment-1", commentData));
    when(dataFetchingEnvironment.<Comment>getSource()).thenReturn(comment);
    when(profileQueryService.findByUsername(eq("jane"), any()))
        .thenReturn(Optional.of(profileData));

    Profile profile = profileDatafetcher.getCommentAuthor(dataFetchingEnvironment);

    assertThat(profile.getUsername()).isEqualTo("jane");
  }

  @Test
  void should_query_profile_by_argument_username() {
    when(dataFetchingEnvironment.<String>getArgument("username")).thenReturn("jane");
    when(profileQueryService.findByUsername(eq("jane"), any()))
        .thenReturn(Optional.of(profileData));

    ProfilePayload payload = profileDatafetcher.queryProfile("jane", dataFetchingEnvironment);

    assertThat(payload.getProfile().getUsername()).isEqualTo("jane");
    assertThat(payload.getProfile().getFollowing()).isTrue();
  }

  @Test
  void should_throw_not_found_when_profile_missing() {
    when(dataFetchingEnvironment.<String>getArgument("username")).thenReturn("ghost");
    when(profileQueryService.findByUsername(eq("ghost"), any())).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> profileDatafetcher.queryProfile("ghost", dataFetchingEnvironment));
  }
}
