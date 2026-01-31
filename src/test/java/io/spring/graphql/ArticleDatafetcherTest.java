package io.spring.graphql;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ArticleDatafetcher.class,
      ProfileDatafetcher.class,
      CommentDatafetcher.class
    })
@Import({TestSecurityConfig.class})
public class ArticleDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;

    @MockBean private UserRepository userRepository;

    // MockBeans for other DgsComponents discovered by DGS auto-configuration
    @MockBean private CommentQueryService commentQueryService;
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

    @BeforeEach
    public void setUp() {
      user = new User("john@jacob.com", "johnjacob", "123", "bio", "image.jpg");
      articleData = createArticleData("test-slug", "Test Title", user);
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

  @Test
  public void should_get_article_by_slug() {
    when(articleQueryService.findBySlug(eq("test-slug"), any())).thenReturn(Optional.of(articleData));

    String query = "query { article(slug: \"test-slug\") { slug title description body tagList favorited favoritesCount } }";

    String slug = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.slug");
    String title = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.title");
    Integer favoritesCount = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.favoritesCount");

    assertThat(slug, equalTo("test-slug"));
    assertThat(title, equalTo("Test Title"));
    assertThat(favoritesCount, equalTo(5));
  }

  @Test
  public void should_fail_get_article_when_not_found() {
    when(articleQueryService.findBySlug(eq("nonexistent"), any())).thenReturn(Optional.empty());

    String query = "query { article(slug: \"nonexistent\") { slug } }";

    var result = dgsQueryExecutor.execute(query);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  @Test
  public void should_get_articles_with_forward_pagination() {
    ArticleData article1 = createArticleData("slug-1", "Title 1", user);
    ArticleData article2 = createArticleData("slug-2", "Title 2", user);
    List<ArticleData> articles = Arrays.asList(article1, article2);
    
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, true);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query = "query { articles(first: 10) { pageInfo { hasNextPage hasPreviousPage } edges { cursor node { slug title } } } }";

    Boolean hasNextPage = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasNextPage");
    Boolean hasPreviousPage = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasPreviousPage");
    List<Object> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(hasNextPage, equalTo(true));
    assertThat(hasPreviousPage, equalTo(false));
    assertThat(edges.size(), equalTo(2));
  }

  @Test
  public void should_get_articles_with_backward_pagination() {
    ArticleData article1 = createArticleData("slug-1", "Title 1", user);
    List<ArticleData> articles = Arrays.asList(article1);
    
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.PREV, true);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query = "query { articles(last: 5) { pageInfo { hasNextPage hasPreviousPage } edges { node { slug } } } }";

    Boolean hasNextPage = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasNextPage");
    Boolean hasPreviousPage = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasPreviousPage");

    assertThat(hasNextPage, equalTo(false));
    assertThat(hasPreviousPage, equalTo(true));
  }

  @Test
  public void should_get_articles_filtered_by_author() {
    ArticleData article1 = createArticleData("slug-1", "Title 1", user);
    List<ArticleData> articles = Arrays.asList(article1);
    
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), eq("johnjacob"), any(), any(), any()))
        .thenReturn(cursorPager);

    String query = "query { articles(first: 10, authoredBy: \"johnjacob\") { edges { node { slug } } } }";

    List<Object> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges.size(), equalTo(1));
  }

  @Test
  public void should_get_articles_filtered_by_tag() {
    ArticleData article1 = createArticleData("slug-1", "Title 1", user);
    List<ArticleData> articles = Arrays.asList(article1);
    
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(eq("tag1"), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query = "query { articles(first: 10, withTag: \"tag1\") { edges { node { slug tagList } } } }";

    List<Object> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges.size(), equalTo(1));
  }

  @Test
  public void should_get_articles_filtered_by_favorited_by() {
    ArticleData article1 = createArticleData("slug-1", "Title 1", user);
    List<ArticleData> articles = Arrays.asList(article1);
    
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), eq("johnjacob"), any(), any()))
        .thenReturn(cursorPager);

    String query = "query { articles(first: 10, favoritedBy: \"johnjacob\") { edges { node { slug } } } }";

    List<Object> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges.size(), equalTo(1));
  }

  @Test
  public void should_fail_articles_query_without_pagination_params() {
    String query = "query { articles { edges { node { slug } } } }";

    var result = dgsQueryExecutor.execute(query);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  @Test
  public void should_get_feed_for_authenticated_user() {
    setAuthentication(user);
    ArticleData article1 = createArticleData("slug-1", "Title 1", user);
    List<ArticleData> articles = Arrays.asList(article1);
    
    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(cursorPager);

    String query = "query { feed(first: 10) { pageInfo { hasNextPage } edges { node { slug title } } } }";

    List<Object> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.feed.edges");

    assertThat(edges.size(), equalTo(1));
  }

  @Test
  public void should_get_empty_feed_for_unauthenticated_user() {
    CursorPager<ArticleData> cursorPager = new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(cursorPager);

    String query = "query { feed(first: 10) { edges { node { slug } } } }";

    List<Object> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.feed.edges");

    assertThat(edges.size(), equalTo(0));
  }

    @Test
    public void should_get_article_with_author_profile() {
      when(articleQueryService.findBySlug(eq("test-slug"), any())).thenReturn(Optional.of(articleData));
      // Mock profileQueryService to return the author's profile
      ProfileData authorProfile = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
      when(profileQueryService.findByUsername(eq("johnjacob"), any())).thenReturn(Optional.of(authorProfile));

      String query = "query { article(slug: \"test-slug\") { slug author { username bio image following } } }";

      String slug = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.slug");
      String authorUsername = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.author.username");

      assertThat(slug, equalTo("test-slug"));
      assertThat(authorUsername, equalTo("johnjacob"));
    }

  @Test
  public void should_get_empty_articles_list() {
    CursorPager<ArticleData> cursorPager = new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query = "query { articles(first: 10) { pageInfo { hasNextPage hasPreviousPage } edges { node { slug } } } }";

    List<Object> edges = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");
    Boolean hasNextPage = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasNextPage");

    assertThat(edges.size(), equalTo(0));
    assertThat(hasNextPage, equalTo(false));
  }

  private void setAuthentication(User user) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(user, null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
