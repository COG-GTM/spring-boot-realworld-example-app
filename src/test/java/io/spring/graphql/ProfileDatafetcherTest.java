package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.schema.DataFetchingEnvironment;
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
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class ProfileDatafetcherTest {

  private ProfileQueryService profileQueryService;
  private ProfileDatafetcher profileDatafetcher;

  @BeforeEach
  void setUp() {
    profileQueryService = mock(ProfileQueryService.class);
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    when(profileQueryService.findByUsername(eq("target"), any()))
        .thenReturn(Optional.of(new ProfileData("id", "target", "bio", "image", true)));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_query_profile_payload() {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getArgument("username")).thenReturn("target");

    ProfilePayload payload = profileDatafetcher.queryProfile("target", dfe);

    Profile profile = payload.getProfile();
    assertEquals("target", profile.getUsername());
    assertTrue(profile.getFollowing());
  }

  @Test
  void should_throw_when_profile_not_found() {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getArgument("username")).thenReturn("missing");
    when(profileQueryService.findByUsername(eq("missing"), any())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> profileDatafetcher.queryProfile("missing", dfe));
  }

  @Test
  void should_get_user_profile_from_local_context() {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    io.spring.core.user.User user = new io.spring.core.user.User("t@e.com", "target", "p", "", "");
    when(dfe.getLocalContext()).thenReturn(user);

    Profile profile = profileDatafetcher.getUserProfile(dfe);

    assertEquals("target", profile.getUsername());
  }

  @Test
  void should_get_author_of_article() {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    Article article = Article.newBuilder().slug("a-slug").build();
    ArticleData articleData = new ArticleData();
    articleData.setProfileData(new ProfileData("id", "target", "bio", "image", true));
    Map<String, ArticleData> map = Collections.singletonMap("a-slug", articleData);
    when(dfe.getSource()).thenReturn(article);
    when(dfe.getLocalContext()).thenReturn(map);

    Profile profile = profileDatafetcher.getAuthor(dfe);

    assertEquals("target", profile.getUsername());
  }

  @Test
  void should_get_author_of_comment() {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    Comment comment = Comment.newBuilder().id("cid").build();
    CommentData commentData = new CommentData();
    commentData.setProfileData(new ProfileData("id", "target", "bio", "image", true));
    Map<String, CommentData> map = Collections.singletonMap("cid", commentData);
    when(dfe.getSource()).thenReturn(comment);
    when(dfe.getLocalContext()).thenReturn(map);

    Profile profile = profileDatafetcher.getCommentAuthor(dfe);

    assertEquals("target", profile.getUsername());
  }
}
