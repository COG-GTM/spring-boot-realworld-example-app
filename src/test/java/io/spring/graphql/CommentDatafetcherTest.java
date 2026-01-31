package io.spring.graphql;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.ProfileQueryService;
import io.spring.application.TagsQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.application.user.UserService;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.CommentRepository;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      CommentDatafetcher.class,
      ArticleDatafetcher.class,
      ProfileDatafetcher.class
    })
@Import({TestSecurityConfig.class})
public class CommentDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private CommentQueryService commentQueryService;

  @MockBean private ArticleQueryService articleQueryService;

    @MockBean private UserRepository userRepository;

    // MockBeans for other DgsComponents discovered by DGS auto-configuration
    @MockBean private ArticleCommandService articleCommandService;
    @MockBean private ArticleRepository articleRepository;
    @MockBean private ArticleFavoriteRepository articleFavoriteRepository;
    @MockBean private CommentRepository commentRepository;
    @MockBean private UserQueryService userQueryService;
    @MockBean private JwtService jwtService;
    @MockBean private ProfileQueryService profileQueryService;
    @MockBean private TagsQueryService tagsQueryService;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private UserService userService;

    private User user;
  private ArticleData articleData;
  private CommentData commentData;

    @BeforeEach
    public void setUp() {
      user = new User("john@jacob.com", "johnjacob", "123", "bio", "image.jpg");
      articleData = createArticleData("test-slug", "Test Title", user);
      commentData = createCommentData("comment-1", "Test comment body", user);
      // Set up anonymous authentication to avoid NullPointerException in SecurityUtil.getCurrentUser()
      AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken(
          "anonymous", "anonymousUser", java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
      SecurityContextHolder.getContext().setAuthentication(anonymousAuth);
    }

  private ArticleData createArticleData(String slug, String title, User author) {
    DateTime now = DateTime.now();
    return new ArticleData(
        "article-id",
        slug,
        title,
        "Test Description",
        "Test Body",
        false,
        5,
        now,
        now,
        Arrays.asList("tag1", "tag2"),
        new ProfileData(author.getId(), author.getUsername(), author.getBio(), author.getImage(), false));
  }

  private CommentData createCommentData(String id, String body, User author) {
    DateTime now = DateTime.now();
    return new CommentData(
        id,
        body,
        articleData.getId(),
        now,
        now,
        new ProfileData(author.getId(), author.getUsername(), author.getBio(), author.getImage(), false));
  }

  @Test
  public void should_get_article_comments_with_forward_pagination() {
    CommentData comment1 = createCommentData("comment-1", "First comment", user);
    CommentData comment2 = createCommentData("comment-2", "Second comment", user);
    List<CommentData> comments = Arrays.asList(comment1, comment2);

    when(articleQueryService.findBySlug(eq("test-slug"), any())).thenReturn(Optional.of(articleData));
    CursorPager<CommentData> cursorPager = new CursorPager<>(comments, Direction.NEXT, true);
    when(commentQueryService.findByArticleIdWithCursor(any(), any(), any())).thenReturn(cursorPager);

    String query =
        "query { article(slug: \"test-slug\") { slug comments(first: 10) { pageInfo { hasNextPage hasPreviousPage } edges { cursor node { id body } } } } }";

    Boolean hasNextPage =
        dgsQueryExecutor.executeAndExtractJsonPath(
            query, "data.article.comments.pageInfo.hasNextPage");
    Boolean hasPreviousPage =
        dgsQueryExecutor.executeAndExtractJsonPath(
            query, "data.article.comments.pageInfo.hasPreviousPage");
    List<Object> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.comments.edges");

    assertThat(hasNextPage, equalTo(true));
    assertThat(hasPreviousPage, equalTo(false));
    assertThat(edges.size(), equalTo(2));
  }

  @Test
  public void should_get_article_comments_with_backward_pagination() {
    CommentData comment1 = createCommentData("comment-1", "First comment", user);
    List<CommentData> comments = Arrays.asList(comment1);

    when(articleQueryService.findBySlug(eq("test-slug"), any())).thenReturn(Optional.of(articleData));
    CursorPager<CommentData> cursorPager = new CursorPager<>(comments, Direction.PREV, true);
    when(commentQueryService.findByArticleIdWithCursor(any(), any(), any())).thenReturn(cursorPager);

    String query =
        "query { article(slug: \"test-slug\") { comments(last: 5) { pageInfo { hasNextPage hasPreviousPage } edges { node { id } } } } }";

    Boolean hasNextPage =
        dgsQueryExecutor.executeAndExtractJsonPath(
            query, "data.article.comments.pageInfo.hasNextPage");
    Boolean hasPreviousPage =
        dgsQueryExecutor.executeAndExtractJsonPath(
            query, "data.article.comments.pageInfo.hasPreviousPage");

    assertThat(hasNextPage, equalTo(false));
    assertThat(hasPreviousPage, equalTo(true));
  }

  @Test
  public void should_get_empty_comments_list() {
    when(articleQueryService.findBySlug(eq("test-slug"), any())).thenReturn(Optional.of(articleData));
    CursorPager<CommentData> cursorPager = new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);
    when(commentQueryService.findByArticleIdWithCursor(any(), any(), any())).thenReturn(cursorPager);

    String query =
        "query { article(slug: \"test-slug\") { comments(first: 10) { pageInfo { hasNextPage } edges { node { id } } } } }";

    List<Object> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.comments.edges");
    Boolean hasNextPage =
        dgsQueryExecutor.executeAndExtractJsonPath(
            query, "data.article.comments.pageInfo.hasNextPage");

    assertThat(edges.size(), equalTo(0));
    assertThat(hasNextPage, equalTo(false));
  }

  @Test
  public void should_fail_comments_query_without_pagination_params() {
    when(articleQueryService.findBySlug(eq("test-slug"), any())).thenReturn(Optional.of(articleData));

    String query = "query { article(slug: \"test-slug\") { comments { edges { node { id } } } } }";

    var result = dgsQueryExecutor.execute(query);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

    @Test
    public void should_get_comment_with_author_profile() {
      CommentData comment1 = createCommentData("comment-1", "Test comment", user);
      List<CommentData> comments = Arrays.asList(comment1);

      when(articleQueryService.findBySlug(eq("test-slug"), any())).thenReturn(Optional.of(articleData));
      CursorPager<CommentData> cursorPager = new CursorPager<>(comments, Direction.NEXT, false);
      when(commentQueryService.findByArticleIdWithCursor(any(), any(), any())).thenReturn(cursorPager);
      // Mock profileQueryService to return the author's profile
      ProfileData authorProfile = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
      when(profileQueryService.findByUsername(eq("johnjacob"), any())).thenReturn(Optional.of(authorProfile));

      String query =
          "query { article(slug: \"test-slug\") { comments(first: 10) { edges { node { id body author { username bio } } } } } }";

      String authorUsername =
          dgsQueryExecutor.executeAndExtractJsonPath(
              query, "data.article.comments.edges[0].node.author.username");

      assertThat(authorUsername, equalTo("johnjacob"));
    }

  @Test
  public void should_get_comment_timestamps() {
    CommentData comment1 = createCommentData("comment-1", "Test comment", user);
    List<CommentData> comments = Arrays.asList(comment1);

    when(articleQueryService.findBySlug(eq("test-slug"), any())).thenReturn(Optional.of(articleData));
    CursorPager<CommentData> cursorPager = new CursorPager<>(comments, Direction.NEXT, false);
    when(commentQueryService.findByArticleIdWithCursor(any(), any(), any())).thenReturn(cursorPager);

    String query =
        "query { article(slug: \"test-slug\") { comments(first: 10) { edges { node { id createdAt updatedAt } } } } }";

    String createdAt =
        dgsQueryExecutor.executeAndExtractJsonPath(
            query, "data.article.comments.edges[0].node.createdAt");
    String updatedAt =
        dgsQueryExecutor.executeAndExtractJsonPath(
            query, "data.article.comments.edges[0].node.updatedAt");

    assertThat(createdAt != null, equalTo(true));
    assertThat(updatedAt != null, equalTo(true));
  }
}
