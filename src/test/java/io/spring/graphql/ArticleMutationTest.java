package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.application.ArticleQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.article.NewArticleParam;
import io.spring.application.article.UpdateArticleParam;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ArticleMutation.class,
      ArticleDatafetcher.class,
      ProfileDatafetcher.class
    })
public class ArticleMutationTest extends DgsTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleCommandService articleCommandService;
  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;
  @MockBean private ArticleRepository articleRepository;
  @MockBean private ArticleQueryService articleQueryService;
  @MockBean private UserRepository userRepository;
  @MockBean private ProfileQueryService profileQueryService;

  private User user;
  private Article article;

  @BeforeEach
  void setUp() {
    user = TestHelper.userFixture("author");
    article = TestHelper.articleFixture("mutation", user);
    authenticate(user);
  }

  private void stubArticlePayloadResolution() {
    when(articleQueryService.findById(eq(article.getId()), eq(user)))
        .thenReturn(Optional.of(TestHelper.getArticleDataFromArticleAndUser(article, user)));
    when(profileQueryService.findByUsername(eq(user.getUsername()), eq(user)))
        .thenReturn(Optional.of(TestHelper.profileDataFixture(user)));
  }

  @Test
  public void should_create_article() {
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(article);
    stubArticlePayloadResolution();

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { createArticle(input: {title: \"new title\", description: \"desc\", body:"
                + " \"body\", tagList: [\"java\"]}) { article { slug title author { username } }"
                + " } }",
            "data.createArticle.article.slug");

    assertThat(slug).isEqualTo(article.getSlug());
    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    assertThat(captor.getValue().getTitle()).isEqualTo("new title");
    assertThat(captor.getValue().getTagList()).containsExactly("java");
  }

  @Test
  public void should_create_article_with_empty_tag_list_when_tags_are_omitted() {
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(article);
    stubArticlePayloadResolution();

    dgsQueryExecutor.executeAndExtractJsonPath(
        "mutation { createArticle(input: {title: \"new title\", description: \"desc\", body:"
            + " \"body\"}) { article { slug } } }",
        "data.createArticle.article.slug");

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    assertThat(captor.getValue().getTagList()).isEmpty();
  }

  @Test
  public void should_not_create_article_for_anonymous_user() {
    authenticateAnonymously();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { createArticle(input: {title: \"t\", description: \"d\", body: \"b\"}) {"
                + " article { slug } } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(articleCommandService, never()).createArticle(any(), any());
  }

  @Test
  public void should_update_article() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any(UpdateArticleParam.class)))
        .thenReturn(article);
    stubArticlePayloadResolution();

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { updateArticle(slug: \""
                + article.getSlug()
                + "\", changes: {title: \"updated\"}) { article { slug } } }",
            "data.updateArticle.article.slug");

    assertThat(slug).isEqualTo(article.getSlug());
    ArgumentCaptor<UpdateArticleParam> captor = ArgumentCaptor.forClass(UpdateArticleParam.class);
    verify(articleCommandService).updateArticle(eq(article), captor.capture());
    assertThat(captor.getValue().getTitle()).isEqualTo("updated");
  }

  @Test
  public void should_not_update_article_of_another_user() {
    User other = TestHelper.userFixture("other");
    Article otherArticle = TestHelper.articleFixture("other", other);
    when(articleRepository.findBySlug(eq(otherArticle.getSlug())))
        .thenReturn(Optional.of(otherArticle));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { updateArticle(slug: \""
                + otherArticle.getSlug()
                + "\", changes: {title: \"updated\"}) { article { slug } } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  public void should_not_update_unknown_article() {
    when(articleRepository.findBySlug(eq("unknown"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { updateArticle(slug: \"unknown\", changes: {title: \"t\"}) { article { slug"
                + " } } }");

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_favorite_article() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    stubArticlePayloadResolution();

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { favoriteArticle(slug: \""
                + article.getSlug()
                + "\") { article { slug } } }",
            "data.favoriteArticle.article.slug");

    assertThat(slug).isEqualTo(article.getSlug());
    ArgumentCaptor<ArticleFavorite> captor = ArgumentCaptor.forClass(ArticleFavorite.class);
    verify(articleFavoriteRepository).save(captor.capture());
    assertThat(captor.getValue().getArticleId()).isEqualTo(article.getId());
    assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
  }

  @Test
  public void should_not_favorite_unknown_article() {
    when(articleRepository.findBySlug(eq("unknown"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { favoriteArticle(slug: \"unknown\") { article { slug } } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(articleFavoriteRepository, never()).save(any());
  }

  @Test
  public void should_unfavorite_article() {
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));
    stubArticlePayloadResolution();

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { unfavoriteArticle(slug: \""
                + article.getSlug()
                + "\") { article { slug } } }",
            "data.unfavoriteArticle.article.slug");

    assertThat(slug).isEqualTo(article.getSlug());
    verify(articleFavoriteRepository).remove(eq(favorite));
  }

  @Test
  public void should_ignore_unfavorite_when_favorite_does_not_exist() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.empty());
    stubArticlePayloadResolution();

    dgsQueryExecutor.executeAndExtractJsonPath(
        "mutation { unfavoriteArticle(slug: \"" + article.getSlug() + "\") { article { slug } } }",
        "data.unfavoriteArticle.article.slug");

    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  public void should_delete_own_article() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { deleteArticle(slug: \"" + article.getSlug() + "\") { success } }",
            "data.deleteArticle.success");

    assertThat(success).isTrue();
    verify(articleRepository).remove(eq(article));
  }

  @Test
  public void should_not_delete_article_of_another_user() {
    Article otherArticle = TestHelper.articleFixture("other", TestHelper.userFixture("other"));
    when(articleRepository.findBySlug(eq(otherArticle.getSlug())))
        .thenReturn(Optional.of(otherArticle));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { deleteArticle(slug: \"" + otherArticle.getSlug() + "\") { success } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(articleRepository, never()).remove(any());
  }

  @Test
  public void should_not_delete_article_for_anonymous_user() {
    authenticateAnonymously();

    ExecutionResult result =
        dgsQueryExecutor.execute("mutation { deleteArticle(slug: \"any\") { success } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(articleRepository, never()).remove(any());
  }

  @Test
  public void should_fail_article_payload_when_article_disappeared() {
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), eq(user))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { createArticle(input: {title: \"t\", description: \"d\", body: \"b\"}) {"
                + " article { slug } } }");

    assertThat(result.getErrors()).isNotEmpty();
  }
}
