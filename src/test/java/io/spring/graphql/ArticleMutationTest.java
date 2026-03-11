package io.spring.graphql;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.TagsQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.application.user.UserService;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.comment.CommentRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
    classes = {DgsAutoConfiguration.class, ArticleMutation.class, ArticleDatafetcher.class})
@Import({TestSecurityConfig.class})
public class ArticleMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleCommandService articleCommandService;

  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;

  @MockBean private ArticleRepository articleRepository;

    @MockBean private ArticleQueryService articleQueryService;

    // MockBeans for other DgsComponents discovered by DGS auto-configuration
    @MockBean private UserRepository userRepository;
    @MockBean private CommentQueryService commentQueryService;
    @MockBean private CommentRepository commentRepository;
    @MockBean private UserQueryService userQueryService;
    @MockBean private JwtService jwtService;
    @MockBean private ProfileQueryService profileQueryService;
    @MockBean private TagsQueryService tagsQueryService;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private UserService userService;

    private User user;
  private User anotherUser;
  private Article article;
  private ArticleData articleData;

    @BeforeEach
    public void setUp() {
      user = new User("john@jacob.com", "johnjacob", "123", "", "");
      anotherUser = new User("another@test.com", "another", "123", "", "");
      article = new Article("Test Title", "Test Description", "Test Body", Arrays.asList("tag1", "tag2"), user.getId());
      articleData = createArticleData(article, user);
      // Set up anonymous authentication to avoid NullPointerException in SecurityUtil.getCurrentUser()
      AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken(
          "anonymous", "anonymousUser", java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
      SecurityContextHolder.getContext().setAuthentication(anonymousAuth);
    }

    private ArticleData createArticleData(Article article, User author) {
      List<String> tagStrings = article.getTags().stream()
          .map(Tag::getName)
          .collect(Collectors.toList());
      return new ArticleData(
          article.getId(),
          article.getSlug(),
          article.getTitle(),
          article.getDescription(),
          article.getBody(),
          false,
          0,
          article.getCreatedAt(),
          article.getUpdatedAt(),
          tagStrings,
          new ProfileData(author.getId(), author.getUsername(), author.getBio(), author.getImage(), false));
    }

  @Test
  public void should_create_article_success() {
    setAuthentication(user);
    when(articleCommandService.createArticle(any(), any())).thenReturn(article);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { createArticle(input: {title: \"Test Title\", description: \"Test Description\", body: \"Test Body\", tagList: [\"tag1\", \"tag2\"]}) { "
            + "article { slug title description body tagList } } }";

    String slug = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createArticle.article.slug");
    String title = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createArticle.article.title");

    assertThat(slug, equalTo(article.getSlug()));
    assertThat(title, equalTo("Test Title"));

    verify(articleCommandService, atLeastOnce()).createArticle(any(), any());
  }

  @Test
  public void should_fail_create_article_without_authentication() {
    String mutation =
        "mutation { createArticle(input: {title: \"Test\", description: \"Test\", body: \"Test\"}) { "
            + "article { slug } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  @Test
  public void should_update_article_success() {
    setAuthentication(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(any(), any())).thenReturn(article);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation =
        String.format(
            "mutation { updateArticle(slug: \"%s\", changes: {title: \"Updated Title\", description: \"Updated Desc\", body: \"Updated Body\"}) { "
                + "article { slug title } } }",
            article.getSlug());

    String slug = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateArticle.article.slug");

    assertThat(slug, equalTo(article.getSlug()));

    verify(articleCommandService, atLeastOnce()).updateArticle(any(), any());
  }

  @Test
  public void should_fail_update_article_when_not_author() {
    setAuthentication(anotherUser);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    String mutation =
        String.format(
            "mutation { updateArticle(slug: \"%s\", changes: {title: \"Updated\"}) { article { slug } } }",
            article.getSlug());

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  @Test
  public void should_fail_update_article_when_not_found() {
    setAuthentication(user);
    when(articleRepository.findBySlug(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation =
        "mutation { updateArticle(slug: \"nonexistent\", changes: {title: \"Updated\"}) { article { slug } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  @Test
  public void should_favorite_article_success() {
    setAuthentication(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation =
        String.format(
            "mutation { favoriteArticle(slug: \"%s\") { article { slug title } } }", article.getSlug());

    String slug = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.favoriteArticle.article.slug");

    assertThat(slug, equalTo(article.getSlug()));

    verify(articleFavoriteRepository, atLeastOnce()).save(any(ArticleFavorite.class));
  }

  @Test
  public void should_unfavorite_article_success() {
    setAuthentication(user);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId()))).thenReturn(Optional.of(favorite));
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation =
        String.format(
            "mutation { unfavoriteArticle(slug: \"%s\") { article { slug title } } }", article.getSlug());

    String slug = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfavoriteArticle.article.slug");

    assertThat(slug, equalTo(article.getSlug()));

    verify(articleFavoriteRepository, atLeastOnce()).remove(any(ArticleFavorite.class));
  }

  @Test
  public void should_unfavorite_article_when_not_favorited() {
    setAuthentication(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId()))).thenReturn(Optional.empty());
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation =
        String.format(
            "mutation { unfavoriteArticle(slug: \"%s\") { article { slug } } }", article.getSlug());

    String slug = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfavoriteArticle.article.slug");

    assertThat(slug, equalTo(article.getSlug()));
  }

  @Test
  public void should_delete_article_success() {
    setAuthentication(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    String mutation =
        String.format("mutation { deleteArticle(slug: \"%s\") { success } }", article.getSlug());

    Boolean success = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteArticle.success");

    assertThat(success, equalTo(true));

    verify(articleRepository, atLeastOnce()).remove(any(Article.class));
  }

  @Test
  public void should_fail_delete_article_when_not_author() {
    setAuthentication(anotherUser);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    String mutation =
        String.format("mutation { deleteArticle(slug: \"%s\") { success } }", article.getSlug());

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  @Test
  public void should_fail_delete_article_when_not_found() {
    setAuthentication(user);
    when(articleRepository.findBySlug(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation = "mutation { deleteArticle(slug: \"nonexistent\") { success } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  private void setAuthentication(User user) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(user, null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
