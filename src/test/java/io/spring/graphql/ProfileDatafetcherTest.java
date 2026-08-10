package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ProfileDatafetcherTest {

  private final ProfileQueryService profileQueryService = mock(ProfileQueryService.class);
  private final ProfileDatafetcher datafetcher = new ProfileDatafetcher(profileQueryService);

  @BeforeEach
  void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  private User authenticate() {
    User user = new User("jake@jake.jake", "jake", "123", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, null));
    return user;
  }

  private ProfileData profileData(String username) {
    return new ProfileData("profile-id", username, "a bio", "an image", true);
  }

  @Test
  void should_query_profile_by_username_argument() {
    User current = authenticate();
    when(profileQueryService.findByUsername(eq("john"), eq(current)))
        .thenReturn(Optional.of(profileData("john")));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getArgument("username")).thenReturn("john");

    // The username is read from the environment argument, not from the method parameter.
    ProfilePayload payload = datafetcher.queryProfile("ignored-parameter", dfe);

    Profile profile = payload.getProfile();
    assertThat(profile.getUsername()).isEqualTo("john");
    assertThat(profile.getBio()).isEqualTo("a bio");
    assertThat(profile.getImage()).isEqualTo("an image");
    assertThat(profile.getFollowing()).isTrue();
  }

  @Test
  void should_query_profile_with_null_current_user_when_anonymous() {
    when(profileQueryService.findByUsername(eq("john"), isNull()))
        .thenReturn(Optional.of(new ProfileData("id", "john", "bio", "image", false)));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getArgument("username")).thenReturn("john");

    ProfilePayload payload = datafetcher.queryProfile("john", dfe);

    assertThat(payload.getProfile().getFollowing()).isFalse();
    verify(profileQueryService).findByUsername("john", null);
  }

  @Test
  void should_throw_not_found_when_profile_is_missing() {
    when(profileQueryService.findByUsername(eq("ghost"), any())).thenReturn(Optional.empty());
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getArgument("username")).thenReturn("ghost");

    assertThatThrownBy(() -> datafetcher.queryProfile("ghost", dfe))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_get_user_profile_from_local_context() {
    User user = authenticate();
    when(profileQueryService.findByUsername(eq("jake"), eq(user)))
        .thenReturn(Optional.of(profileData("jake")));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<User>getLocalContext()).thenReturn(user);

    Profile profile = datafetcher.getUserProfile(dfe);

    assertThat(profile.getUsername()).isEqualTo("jake");
    assertThat(profile.getFollowing()).isTrue();
  }

  @Test
  void should_get_article_author_from_local_context_map() {
    ArticleData articleData =
        new ArticleData(
            "article-id",
            "a-title",
            "a title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.singletonList("java"),
            profileData("author"));
    Map<String, ArticleData> localContext =
        Collections.singletonMap(articleData.getSlug(), articleData);
    when(profileQueryService.findByUsername(eq("author"), isNull()))
        .thenReturn(Optional.of(profileData("author")));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<Map<String, ArticleData>>getLocalContext()).thenReturn(localContext);
    when(dfe.<Article>getSource())
        .thenReturn(Article.newBuilder().slug(articleData.getSlug()).build());

    Profile profile = datafetcher.getAuthor(dfe);

    assertThat(profile.getUsername()).isEqualTo("author");
  }

  @Test
  void should_get_comment_author_from_local_context_map() {
    CommentData commentData =
        new CommentData(
            "comment-id",
            "a comment",
            "article-id",
            new DateTime(),
            new DateTime(),
            profileData("commenter"));
    Map<String, CommentData> localContext = Collections.singletonMap("comment-id", commentData);
    when(profileQueryService.findByUsername(eq("commenter"), isNull()))
        .thenReturn(Optional.of(profileData("commenter")));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<Map<String, CommentData>>getLocalContext()).thenReturn(localContext);
    when(dfe.<Comment>getSource()).thenReturn(Comment.newBuilder().id("comment-id").build());

    Profile profile = datafetcher.getCommentAuthor(dfe);

    assertThat(profile.getUsername()).isEqualTo("commenter");
  }
}
