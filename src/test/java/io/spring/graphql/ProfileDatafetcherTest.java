package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.core.user.User;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.Profile;
import io.spring.graphql.types.ProfilePayload;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileDatafetcherTest extends GraphQLTestBase {

  @Mock private ProfileQueryService profileQueryService;

  private ProfileDatafetcher profileDatafetcher;

  @BeforeEach
  void setUp() {
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);
    setAnonymous();
  }

  @Test
  void should_get_user_profile_from_local_context() {
    DataFetchingEnvironment dfe = mockEnv();
    User user = newUser();
    when(dfe.getLocalContext()).thenReturn(user);
    when(profileQueryService.findByUsername(eq("johnjacob"), any()))
        .thenReturn(Optional.of(profileData("johnjacob")));

    Profile profile = profileDatafetcher.getUserProfile(dfe);

    assertThat(profile.getUsername()).isEqualTo("johnjacob");
    assertThat(profile.getBio()).isEqualTo("some bio");
  }

  @Test
  void should_throw_when_profile_missing() {
    DataFetchingEnvironment dfe = mockEnv();
    User user = newUser();
    when(dfe.getLocalContext()).thenReturn(user);
    when(profileQueryService.findByUsername(eq("johnjacob"), any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> profileDatafetcher.getUserProfile(dfe))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_get_article_author() {
    DataFetchingEnvironment dfe = mockEnv();
    Article article = Article.newBuilder().slug("art-slug").build();
    Map<String, ArticleData> map = new HashMap<>();
    map.put("art-slug", articleData("a1", "art-slug", "authorname"));
    when(dfe.getSource()).thenReturn(article);
    when(dfe.getLocalContext()).thenReturn(map);
    when(profileQueryService.findByUsername(eq("authorname"), any()))
        .thenReturn(Optional.of(profileData("authorname")));

    Profile profile = profileDatafetcher.getAuthor(dfe);

    assertThat(profile.getUsername()).isEqualTo("authorname");
  }

  @Test
  void should_get_comment_author() {
    DataFetchingEnvironment dfe = mockEnv();
    Comment comment = Comment.newBuilder().id("c1").build();
    Map<String, CommentData> map = new HashMap<>();
    map.put("c1", commentData("c1", "art1", "commenter"));
    when(dfe.getSource()).thenReturn(comment);
    when(dfe.getLocalContext()).thenReturn(map);
    when(profileQueryService.findByUsername(eq("commenter"), any()))
        .thenReturn(Optional.of(profileData("commenter")));

    Profile profile = profileDatafetcher.getCommentAuthor(dfe);

    assertThat(profile.getUsername()).isEqualTo("commenter");
  }

  @Test
  void should_query_profile_by_argument() {
    DataFetchingEnvironment dfe = mockEnv();
    when(dfe.getArgument("username")).thenReturn("johnjacob");
    when(profileQueryService.findByUsername(eq("johnjacob"), any()))
        .thenReturn(Optional.of(profileData("johnjacob")));

    ProfilePayload payload = profileDatafetcher.queryProfile("johnjacob", dfe);

    assertThat(payload.getProfile().getUsername()).isEqualTo("johnjacob");
  }
}
