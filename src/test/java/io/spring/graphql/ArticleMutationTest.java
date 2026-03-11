package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.ArticleQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(classes = {DgsAutoConfiguration.class, ArticleMutation.class, ArticleDatafetcher.class, ProfileDatafetcher.class})
public class ArticleMutationTest {

  @Autowired
  private DgsQueryExecutor dgsQueryExecutor;

  @MockBean
  private ArticleCommandService articleCommandService;

  @MockBean
  private ArticleFavoriteRepository articleFavoriteRepository;

  @MockBean
  private ArticleRepository articleRepository;

  @MockBean
  private ArticleQueryService articleQueryService;

  @MockBean
  private ProfileQueryService profileQueryService;

  @MockBean
  private UserRepository userRepository;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User("test@example.com", "testuser", "password", "", "");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(testUser, null, null)
    );
  }

  @Test
  void shouldCreateArticle() {
    DateTime now = DateTime.now();
    Article article = new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java", "spring"), testUser.getId(), now);

    ProfileData profileData = new ProfileData(testUser.getId(), testUser.getUsername(), "", "", false);
    ArticleData articleData = new ArticleData(
        article.getId(),
        article.getSlug(),
        article.getTitle(),
        article.getDescription(),
        article.getBody(),
        false,
        0,
        now,
        now,
        Arrays.asList("java", "spring"),
        profileData
    );

    when(articleCommandService.createArticle(any(), eq(testUser))).thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(testUser.getUsername()), any())).thenReturn(Optional.of(profileData));

    String mutation = "mutation { createArticle(input: { title: \"Test Title\", description: \"Test Description\", body: \"Test Body\", tagList: [\"java\", \"spring\"] }) { article { slug title description body } } }";

    graphql.ExecutionResult executionResult = dgsQueryExecutor.execute(mutation);

    assertThat(executionResult.getErrors()).isEmpty();
    assertThat((Object) executionResult.getData()).isNotNull();
  }

  @Test
  void shouldDeleteArticle() {
    DateTime now = DateTime.now();
    Article article = new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), testUser.getId(), now);

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    String mutation = "mutation { deleteArticle(slug: \"" + article.getSlug() + "\") { success } }";

    Boolean success = dgsQueryExecutor.executeAndExtractJsonPath(
        mutation,
        "data.deleteArticle.success"
    );

    assertThat(success).isTrue();
  }

  @Test
  void shouldFavoriteArticle() {
    DateTime now = DateTime.now();
    Article article = new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), testUser.getId(), now);

    ProfileData profileData = new ProfileData(testUser.getId(), testUser.getUsername(), "", "", false);
    ArticleData articleData = new ArticleData(
        article.getId(),
        article.getSlug(),
        article.getTitle(),
        article.getDescription(),
        article.getBody(),
        true,
        1,
        now,
        now,
        Arrays.asList("java"),
        profileData
    );

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(testUser.getUsername()), any())).thenReturn(Optional.of(profileData));

    String mutation = "mutation { favoriteArticle(slug: \"" + article.getSlug() + "\") { article { slug favorited } } }";

    graphql.ExecutionResult executionResult = dgsQueryExecutor.execute(mutation);

    assertThat(executionResult.getErrors()).isEmpty();
    assertThat((Object) executionResult.getData()).isNotNull();
  }

  @Test
  void shouldUnfavoriteArticle() {
    DateTime now = DateTime.now();
    Article article = new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), testUser.getId(), now);

    ProfileData profileData = new ProfileData(testUser.getId(), testUser.getUsername(), "", "", false);
    ArticleData articleData = new ArticleData(
        article.getId(),
        article.getSlug(),
        article.getTitle(),
        article.getDescription(),
        article.getBody(),
        false,
        0,
        now,
        now,
        Arrays.asList("java"),
        profileData
    );

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(testUser.getId()))).thenReturn(Optional.empty());
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(testUser.getUsername()), any())).thenReturn(Optional.of(profileData));

    String mutation = "mutation { unfavoriteArticle(slug: \"" + article.getSlug() + "\") { article { slug favorited } } }";

    graphql.ExecutionResult executionResult = dgsQueryExecutor.execute(mutation);

    assertThat(executionResult.getErrors()).isEmpty();
    assertThat((Object) executionResult.getData()).isNotNull();
  }
}
