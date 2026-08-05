package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.ProfileQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ProfileDatafetcher.class,
      ArticleDatafetcher.class,
      CommentDatafetcher.class,
      MeDatafetcher.class
    })
public class ProfileDatafetcherTest extends GraphQLTestBase {

  private static final DateTime TIME = new DateTime(2022, 2, 2, 10, 0, DateTimeZone.UTC);

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ProfileQueryService profileQueryService;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private CommentQueryService commentQueryService;

  @MockBean private UserQueryService userQueryService;

  @MockBean private JwtService jwtService;

  @MockBean private io.spring.core.user.UserRepository userRepository;

  @Test
  void should_query_profile_by_username() {
    when(profileQueryService.findByUsername(eq("jane"), eq(user)))
        .thenReturn(Optional.of(new ProfileData("jane-id", "jane", "jane bio", "jane.png", true)));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ profile(username: \"jane\") { profile { username bio image following } } }");

    assertThat(context.read("$.data.profile.profile.username", String.class)).isEqualTo("jane");
    assertThat(context.read("$.data.profile.profile.bio", String.class)).isEqualTo("jane bio");
    assertThat(context.read("$.data.profile.profile.image", String.class)).isEqualTo("jane.png");
    assertThat(context.read("$.data.profile.profile.following", Boolean.class)).isTrue();
  }

  @Test
  void should_query_profile_for_anonymous_user() {
    logout();
    when(profileQueryService.findByUsername(eq("jane"), isNull()))
        .thenReturn(Optional.of(new ProfileData("jane-id", "jane", "jane bio", "jane.png", false)));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ profile(username: \"jane\") { profile { username following } } }");

    assertThat(context.read("$.data.profile.profile.following", Boolean.class)).isFalse();
  }

  @Test
  void should_fail_to_query_unknown_profile() {
    when(profileQueryService.findByUsername(eq("ghost"), eq(user))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute("{ profile(username: \"ghost\") { profile { username } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  @Test
  void should_return_profile_of_current_user() {
    when(userQueryService.findById(eq(user.getId())))
        .thenReturn(
            Optional.of(
                new UserData(
                    user.getId(), user.getEmail(), user.getUsername(), "bio", DEFAULT_AVATAR)));
    when(profileQueryService.findByUsername(eq(user.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileData));

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token jwt-token");
    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ me { username profile { username bio following } } }",
            Collections.emptyMap(),
            headers);

    assertThat(context.read("$.data.me.profile.username", String.class))
        .isEqualTo(user.getUsername());
    assertThat(context.read("$.data.me.profile.following", Boolean.class)).isFalse();
  }

  @Test
  void should_return_article_author_profile() {
    ArticleData articleData = articleData();
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), eq(user)))
        .thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(user.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileData));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ article(slug: \""
                + articleData.getSlug()
                + "\") { slug author { username bio } } }");

    assertThat(context.read("$.data.article.author.username", String.class))
        .isEqualTo(user.getUsername());
    assertThat(context.read("$.data.article.author.bio", String.class)).isEqualTo(user.getBio());
  }

  @Test
  void should_return_comment_author_profile() {
    ArticleData articleData = articleData();
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), eq(user)))
        .thenReturn(Optional.of(articleData));
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), eq(user), any()))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(
                    new CommentData(
                        "comment-id", "a comment", articleData.getId(), TIME, TIME, profileData)),
                Direction.NEXT,
                false));
    when(profileQueryService.findByUsername(eq(user.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileData));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ article(slug: \""
                + articleData.getSlug()
                + "\") { comments(first: 1) { edges { node { id author { username } } } } } }");

    assertThat(context.read("$.data.article.comments.edges[0].node.author.username", String.class))
        .isEqualTo(user.getUsername());
  }

  private ArticleData articleData() {
    return new ArticleData(
        "article-id",
        "a-title",
        "a title",
        "a description",
        "a body",
        false,
        0,
        TIME,
        TIME,
        Arrays.asList("joda"),
        profileData);
  }
}
