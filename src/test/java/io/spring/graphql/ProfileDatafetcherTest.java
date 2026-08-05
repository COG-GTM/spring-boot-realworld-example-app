package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
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
  private User user;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    profileQueryService = mock(ProfileQueryService.class);
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);
    user = new User("john@jacob.com", "johnjacob", "123", "bio", "image");
    profileData = new ProfileData(user.getId(), user.getUsername(), "bio", "image", true);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User currentUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                currentUser, null, AuthorityUtils.NO_AUTHORITIES));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @Test
  public void should_query_profile_of_the_user_in_local_context() {
    authenticate(user);
    when(profileQueryService.findByUsername(eq(user.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileData));
    DataFetchingEnvironment dataFetchingEnvironment = mock(DataFetchingEnvironment.class);
    doReturn(user).when(dataFetchingEnvironment).getLocalContext();

    Profile profile = profileDatafetcher.getUserProfile(dataFetchingEnvironment);

    assertEquals("johnjacob", profile.getUsername());
    assertEquals("bio", profile.getBio());
    assertEquals("image", profile.getImage());
    assertTrue(profile.getFollowing());
  }

  @Test
  public void should_query_profile_without_current_user_when_not_authenticated() {
    anonymous();
    when(profileQueryService.findByUsername(eq("johnjacob"), isNull()))
        .thenReturn(Optional.of(new ProfileData(user.getId(), "johnjacob", "bio", "image", false)));
    DataFetchingEnvironment dataFetchingEnvironment = mock(DataFetchingEnvironment.class);
    when(dataFetchingEnvironment.getArgument(eq("username"))).thenReturn("johnjacob");

    ProfilePayload payload = profileDatafetcher.queryProfile("johnjacob", dataFetchingEnvironment);

    assertEquals("johnjacob", payload.getProfile().getUsername());
    assertFalse(payload.getProfile().getFollowing());
    verify(profileQueryService).findByUsername(eq("johnjacob"), isNull());
  }

  @Test
  public void should_use_username_argument_from_environment_when_querying_profile() {
    authenticate(user);
    when(profileQueryService.findByUsername(eq("other"), eq(user)))
        .thenReturn(Optional.of(new ProfileData("other-id", "other", "b", "i", true)));
    DataFetchingEnvironment dataFetchingEnvironment = mock(DataFetchingEnvironment.class);
    when(dataFetchingEnvironment.getArgument(eq("username"))).thenReturn("other");

    ProfilePayload payload = profileDatafetcher.queryProfile("other", dataFetchingEnvironment);

    assertEquals("other", payload.getProfile().getUsername());
    assertTrue(payload.getProfile().getFollowing());
  }

  @Test
  public void should_throw_not_found_when_profile_does_not_exist() {
    anonymous();
    when(profileQueryService.findByUsername(eq("ghost"), isNull())).thenReturn(Optional.empty());
    DataFetchingEnvironment dataFetchingEnvironment = mock(DataFetchingEnvironment.class);
    when(dataFetchingEnvironment.getArgument(eq("username"))).thenReturn("ghost");

    assertThrows(
        ResourceNotFoundException.class,
        () -> profileDatafetcher.queryProfile("ghost", dataFetchingEnvironment));
  }

  @Test
  public void should_query_author_profile_of_an_article() {
    authenticate(user);
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
            null,
            profileData);
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put("a-title", articleData);
    when(profileQueryService.findByUsername(eq(user.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileData));
    DataFetchingEnvironment dataFetchingEnvironment = mock(DataFetchingEnvironment.class);
    doReturn(localContext).when(dataFetchingEnvironment).getLocalContext();
    doReturn(Article.newBuilder().slug("a-title").build())
        .when(dataFetchingEnvironment)
        .getSource();

    Profile author = profileDatafetcher.getAuthor(dataFetchingEnvironment);

    assertEquals("johnjacob", author.getUsername());
    assertTrue(author.getFollowing());
  }

  @Test
  public void should_query_author_profile_of_a_comment() {
    authenticate(user);
    CommentData commentData =
        new CommentData(
            "comment-id", "body", "article-id", new DateTime(), new DateTime(), profileData);
    Map<String, CommentData> localContext = new HashMap<>();
    localContext.put("comment-id", commentData);
    when(profileQueryService.findByUsername(eq(user.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileData));
    DataFetchingEnvironment dataFetchingEnvironment = mock(DataFetchingEnvironment.class);
    doReturn(localContext).when(dataFetchingEnvironment).getLocalContext();
    doReturn(Comment.newBuilder().id("comment-id").build())
        .when(dataFetchingEnvironment)
        .getSource();

    Profile author = profileDatafetcher.getCommentAuthor(dataFetchingEnvironment);

    assertEquals("johnjacob", author.getUsername());
    assertEquals("bio", author.getBio());
  }

  @Test
  public void should_fail_when_comment_is_missing_from_local_context() {
    anonymous();
    Map<String, CommentData> localContext = new HashMap<>();
    DataFetchingEnvironment dataFetchingEnvironment = mock(DataFetchingEnvironment.class);
    doReturn(localContext).when(dataFetchingEnvironment).getLocalContext();
    doReturn(Comment.newBuilder().id("unknown-id").build())
        .when(dataFetchingEnvironment)
        .getSource();

    assertThrows(
        NullPointerException.class,
        () -> profileDatafetcher.getCommentAuthor(dataFetchingEnvironment));
  }
}
