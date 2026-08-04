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
import io.spring.application.article.ArticleCommandService;
import io.spring.application.article.NewArticleParam;
import io.spring.application.data.ArticleData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {DgsAutoConfiguration.class, ArticleMutation.class, ArticleDatafetcher.class})
class ArticleMutationTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleCommandService articleCommandService;
  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;
  @MockBean private ArticleRepository articleRepository;
  // Collaborators of ArticleDatafetcher, imported to resolve ArticlePayload.article.
  @MockBean private ArticleQueryService articleQueryService;
  @MockBean private UserRepository userRepository;

  private final User author = new User("a@example.com", "author", "123", "", "");
  private final User other = new User("o@example.com", "other", "123", "", "");

  private Article article() {
    return new Article("Title", "Desc", "Body", Arrays.asList("java"), author.getId());
  }

  @Test
  void should_create_article_for_authenticated_user() {
    authenticate(author);
    Article created = article();
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(author)))
        .thenReturn(created);

    String query =
        "mutation { createArticle(input: {title: \"Title\", description: \"Desc\", body: \"Body\","
            + " tagList: [\"java\"]}) { __typename } }";

    String typename =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.createArticle.__typename");

    assertThat(typename).isEqualTo("ArticlePayload");
    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(author));
    assertThat(captor.getValue().getTitle()).isEqualTo("Title");
    assertThat(captor.getValue().getTagList()).containsExactly("java");
  }

  @Test
  void should_resolve_payload_article_from_mutation_local_context() {
    authenticate(author);
    Article created = article();
    ArticleData data = TestHelper.getArticleDataFromArticleAndUser(created, author);
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(author)))
        .thenReturn(created);
    when(articleQueryService.findById(eq(created.getId()), eq(author)))
        .thenReturn(Optional.of(data));

    String query =
        "mutation { createArticle(input: {title: \"Title\", description: \"Desc\", body: \"Body\"})"
            + " { article { slug title description body tagList } } }";

    assertThat(
            dgsQueryExecutor.<String>executeAndExtractJsonPath(
                query, "data.createArticle.article.slug"))
        .isEqualTo(created.getSlug());
    assertThat(
            dgsQueryExecutor.<String>executeAndExtractJsonPath(
                query, "data.createArticle.article.title"))
        .isEqualTo("Title");
    assertThat(
            dgsQueryExecutor.<List<String>>executeAndExtractJsonPath(
                query, "data.createArticle.article.tagList"))
        .containsExactly("joda");
  }

  @Test
  void should_error_resolving_payload_article_when_article_disappeared() {
    authenticate(author);
    Article created = article();
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(author)))
        .thenReturn(created);
    when(articleQueryService.findById(eq(created.getId()), eq(author)))
        .thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { createArticle(input: {title: \"Title\", description: \"Desc\","
                + " body: \"Body\"}) { article { slug } } }");

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void should_error_creating_article_when_unauthenticated() {
    anonymous();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { createArticle(input: {title: \"T\", description: \"D\", body: \"B\"})"
                + " { __typename } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(articleCommandService, never()).createArticle(any(), any());
  }

  @Test
  void should_update_article_when_author() {
    authenticate(author);
    Article article = article();
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);

    String query =
        "mutation { updateArticle(slug: \""
            + article.getSlug()
            + "\", changes: {title: \"New Title\"}) { __typename } }";

    String typename =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.updateArticle.__typename");

    assertThat(typename).isEqualTo("ArticlePayload");
    verify(articleCommandService).updateArticle(eq(article), any());
  }

  @Test
  void should_reject_update_when_not_author() {
    authenticate(other);
    Article article = article();
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { updateArticle(slug: \""
                + article.getSlug()
                + "\", changes: {title: \"New\"}) { __typename } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  void should_error_update_when_article_missing() {
    authenticate(author);
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { updateArticle(slug: \"missing\", changes: {title: \"New\"}) { __typename } }");

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void should_favorite_article() {
    authenticate(author);
    Article article = article();
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    String query =
        "mutation { favoriteArticle(slug: \"" + article.getSlug() + "\") { __typename } }";
    String typename =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.favoriteArticle.__typename");

    assertThat(typename).isEqualTo("ArticlePayload");
    ArgumentCaptor<ArticleFavorite> captor = ArgumentCaptor.forClass(ArticleFavorite.class);
    verify(articleFavoriteRepository).save(captor.capture());
    assertThat(captor.getValue().getArticleId()).isEqualTo(article.getId());
    assertThat(captor.getValue().getUserId()).isEqualTo(author.getId());
  }

  @Test
  void should_unfavorite_article_when_favorite_exists() {
    authenticate(author);
    Article article = article();
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), author.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(author.getId())))
        .thenReturn(Optional.of(favorite));

    String query =
        "mutation { unfavoriteArticle(slug: \"" + article.getSlug() + "\") { __typename } }";
    dgsQueryExecutor.executeAndExtractJsonPath(query, "data.unfavoriteArticle.__typename");

    verify(articleFavoriteRepository).remove(eq(favorite));
  }

  @Test
  void should_delete_article_when_author() {
    authenticate(author);
    Article article = article();
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { deleteArticle(slug: \"" + article.getSlug() + "\") { success } }",
            "data.deleteArticle.success");

    assertThat(success).isTrue();
    verify(articleRepository).remove(eq(article));
  }

  @Test
  void should_reject_delete_when_not_author() {
    authenticate(other);
    Article article = article();
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { deleteArticle(slug: \"" + article.getSlug() + "\") { success } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(articleRepository, never()).remove(any());
  }
}
