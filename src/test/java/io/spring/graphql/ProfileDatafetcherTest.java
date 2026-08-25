package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private ProfileDatafetcher profileDatafetcher;
  private User user;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);
    user = new User("a@test.com", "aisensiy", "123", "bio", "image");
    profileData = new ProfileData("user-id", "aisensiy", "bio", "image", true);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_get_user_profile_from_local_context() {
    when(dataFetchingEnvironment.<User>getLocalContext()).thenReturn(user);
    when(profileQueryService.findByUsername("aisensiy", null)).thenReturn(Optional.of(profileData));

    Profile profile = profileDatafetcher.getUserProfile(dataFetchingEnvironment);

    assertThat(profile.getUsername()).isEqualTo("aisensiy");
    assertThat(profile.getBio()).isEqualTo("bio");
    assertThat(profile.getImage()).isEqualTo("image");
    assertThat(profile.getFollowing()).isTrue();
  }

  @Test
  public void should_use_current_user_when_authenticated() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    when(dataFetchingEnvironment.<User>getLocalContext()).thenReturn(user);
    when(profileQueryService.findByUsername("aisensiy", user)).thenReturn(Optional.of(profileData));

    assertThat(profileDatafetcher.getUserProfile(dataFetchingEnvironment).getUsername())
        .isEqualTo("aisensiy");
  }

  @Test
  public void should_throw_when_profile_not_found() {
    when(dataFetchingEnvironment.<User>getLocalContext()).thenReturn(user);
    when(profileQueryService.findByUsername("aisensiy", null)).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> profileDatafetcher.getUserProfile(dataFetchingEnvironment));
  }

  @Test
  public void should_get_article_author() {
    Article article = Article.newBuilder().slug("a-slug").build();
    ArticleData articleData = new ArticleData();
    articleData.setSlug("a-slug");
    articleData.setProfileData(profileData);
    Map<String, ArticleData> localContext = Collections.singletonMap("a-slug", articleData);
    when(dataFetchingEnvironment.<Map<String, ArticleData>>getLocalContext())
        .thenReturn(localContext);
    when(dataFetchingEnvironment.<Article>getSource()).thenReturn(article);
    when(profileQueryService.findByUsername("aisensiy", null)).thenReturn(Optional.of(profileData));

    assertThat(profileDatafetcher.getAuthor(dataFetchingEnvironment).getUsername())
        .isEqualTo("aisensiy");
  }

  @Test
  public void should_get_comment_author() {
    Comment comment = Comment.newBuilder().id("comment-id").build();
    CommentData commentData = new CommentData();
    commentData.setId("comment-id");
    commentData.setProfileData(profileData);
    Map<String, CommentData> localContext = Collections.singletonMap("comment-id", commentData);
    when(dataFetchingEnvironment.<Map<String, CommentData>>getLocalContext())
        .thenReturn(localContext);
    when(dataFetchingEnvironment.<Comment>getSource()).thenReturn(comment);
    when(profileQueryService.findByUsername("aisensiy", null)).thenReturn(Optional.of(profileData));

    assertThat(profileDatafetcher.getCommentAuthor(dataFetchingEnvironment).getUsername())
        .isEqualTo("aisensiy");
  }

  @Test
  public void should_query_profile_by_username_argument() {
    when(dataFetchingEnvironment.<String>getArgument("username")).thenReturn("aisensiy");
    when(profileQueryService.findByUsername("aisensiy", null)).thenReturn(Optional.of(profileData));

    ProfilePayload payload = profileDatafetcher.queryProfile("aisensiy", dataFetchingEnvironment);

    assertThat(payload.getProfile().getUsername()).isEqualTo("aisensiy");
    assertThat(payload.getProfile().getFollowing()).isTrue();
  }
}
