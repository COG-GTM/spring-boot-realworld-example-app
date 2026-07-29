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
import io.spring.TestHelper;
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
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ProfileDatafetcher.class,
      ArticleDatafetcher.class,
      CommentDatafetcher.class,
      MeDatafetcher.class
    })
public class ProfileDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ProfileQueryService profileQueryService;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private CommentQueryService commentQueryService;

  @MockBean private UserQueryService userQueryService;

  @MockBean private UserRepository userRepository;

  @MockBean private JwtService jwtService;

  private User currentUser;
  private User author;
  private ArticleData articleData;

  @BeforeEach
  public void setUp() {
    currentUser = new User("current@test.com", "current", "123", "", "");
    author = new User("author@test.com", "author", "123", "author bio", "author image");
    articleData = TestHelper.articleDataFixture("1", author);
    authenticate(currentUser);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_query_a_profile_by_username() {
    mockAuthorProfile(currentUser, true);

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "{ profile(username: \"%s\") { profile { username bio image following } } }",
                author.getUsername()));

    assertThat(result.<String>read("data.profile.profile.username"))
        .isEqualTo(author.getUsername());
    assertThat(result.<String>read("data.profile.profile.bio")).isEqualTo(author.getBio());
    assertThat(result.<String>read("data.profile.profile.image")).isEqualTo(author.getImage());
    assertThat(result.<Boolean>read("data.profile.profile.following")).isTrue();
  }

  @Test
  public void should_query_a_profile_without_current_user_when_anonymous() {
    anonymous();
    mockAuthorProfile(null, false);

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "{ profile(username: \"%s\") { profile { username following } } }",
                author.getUsername()));

    assertThat(result.<String>read("data.profile.profile.username"))
        .isEqualTo(author.getUsername());
    assertThat(result.<Boolean>read("data.profile.profile.following")).isFalse();
  }

  @Test
  public void should_report_error_when_the_profile_does_not_exist() {
    when(profileQueryService.findByUsername(eq("ghost"), any())).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute("{ profile(username: \"ghost\") { profile { username } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  @Test
  public void should_resolve_the_profile_of_the_current_user() {
    when(userQueryService.findById(eq(currentUser.getId())))
        .thenReturn(
            Optional.of(
                new UserData(
                    currentUser.getId(),
                    currentUser.getEmail(),
                    currentUser.getUsername(),
                    "",
                    "")));
    when(profileQueryService.findByUsername(eq(currentUser.getUsername()), eq(currentUser)))
        .thenReturn(
            Optional.of(
                new ProfileData(
                    currentUser.getId(), currentUser.getUsername(), "my bio", "my image", false)));

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token jwt-token");
    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ me { username profile { username bio image following } } }",
            Collections.emptyMap(),
            headers);

    assertThat(result.<String>read("data.me.profile.username"))
        .isEqualTo(currentUser.getUsername());
    assertThat(result.<String>read("data.me.profile.bio")).isEqualTo("my bio");
    assertThat(result.<Boolean>read("data.me.profile.following")).isFalse();
  }

  @Test
  public void should_resolve_the_author_of_an_article() {
    mockAuthorProfile(currentUser, true);
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), eq(currentUser)))
        .thenReturn(Optional.of(articleData));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "{ article(slug: \"%s\") { slug author { username bio following } } }",
                articleData.getSlug()));

    assertThat(result.<String>read("data.article.author.username")).isEqualTo(author.getUsername());
    assertThat(result.<String>read("data.article.author.bio")).isEqualTo(author.getBio());
    assertThat(result.<Boolean>read("data.article.author.following")).isTrue();
  }

  @Test
  public void should_resolve_the_author_of_a_comment() {
    mockAuthorProfile(currentUser, false);
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), eq(currentUser)))
        .thenReturn(Optional.of(articleData));
    DateTime now = new DateTime();
    CommentData commentData =
        new CommentData(
            "comment-1",
            "a comment",
            articleData.getId(),
            now,
            now,
            new ProfileData(
                author.getId(), author.getUsername(), author.getBio(), author.getImage(), false));
    when(commentQueryService.findByArticleIdWithCursor(
            eq(articleData.getId()), eq(currentUser), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(commentData), Direction.NEXT, false));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "{ article(slug: \"%s\") { comments(first: 1) { edges { node { id author { username"
                    + " bio } } } } } }",
                articleData.getSlug()));

    assertThat(result.<String>read("data.article.comments.edges[0].node.author.username"))
        .isEqualTo(author.getUsername());
    assertThat(result.<String>read("data.article.comments.edges[0].node.author.bio"))
        .isEqualTo(author.getBio());
  }

  private void mockAuthorProfile(User expectedCurrentUser, boolean following) {
    ProfileData profileData =
        new ProfileData(
            author.getId(), author.getUsername(), author.getBio(), author.getImage(), following);
    if (expectedCurrentUser == null) {
      when(profileQueryService.findByUsername(eq(author.getUsername()), isNull()))
          .thenReturn(Optional.of(profileData));
    } else {
      when(profileQueryService.findByUsername(eq(author.getUsername()), eq(expectedCurrentUser)))
          .thenReturn(Optional.of(profileData));
    }
  }

  private void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }
}
