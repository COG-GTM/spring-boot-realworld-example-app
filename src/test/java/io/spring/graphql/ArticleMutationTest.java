package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.application.article.NewArticleParam;
import io.spring.application.article.UpdateArticleParam;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ArticleMutationTest extends GraphQLTestBase {
  private Article article;

  @BeforeEach
  public void setUp() {
    article =
        new Article(
            "How to train your dragon",
            "Ever wonder how?",
            "You have to believe",
            Arrays.asList("dragons", "training"),
            user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleQueryService.findById(eq(article.getId()), any()))
        .thenReturn(Optional.of(TestHelper.getArticleDataFromArticleAndUser(article, user)));
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(
            Optional.of(
                new ProfileData(
                    user.getId(), user.getUsername(), user.getBio(), user.getImage(), false)));
  }

  @Test
  public void should_create_article() {
    loginAs(user);
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    Map<String, Object> input = new HashMap<>();
    input.put("title", article.getTitle());
    input.put("description", article.getDescription());
    input.put("body", article.getBody());
    input.put("tagList", Arrays.asList("dragons", "training"));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation ($input: CreateArticleInput!) { createArticle(input: $input) { article {"
                + " slug title body author { username } } } }",
            singletonVariable("input", input));

    assertThat(context.<String>read("data.createArticle.article.slug"))
        .isEqualTo(article.getSlug());
    assertThat(context.<String>read("data.createArticle.article.title"))
        .isEqualTo(article.getTitle());
    assertThat(context.<String>read("data.createArticle.article.author.username"))
        .isEqualTo(user.getUsername());

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    assertThat(captor.getValue().getTagList()).containsExactly("dragons", "training");
  }

  @Test
  public void should_create_article_without_tags() {
    loginAs(user);
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { createArticle(input: {title: \"t\", description: \"d\", body: \"b\"})"
                + " { article { slug tagList } } }");

    assertThat(context.<String>read("data.createArticle.article.slug"))
        .isEqualTo(article.getSlug());
    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    assertThat(captor.getValue().getTagList()).isEmpty();
  }

  @Test
  public void should_reject_create_article_without_authentication() {
    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { createArticle(input: {title: \"t\", description: \"d\", body: \"b\"}) {"
                + " article { slug } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getPath()).containsExactly("createArticle");
    verify(articleCommandService, never()).createArticle(any(), any());
  }

  @Test
  public void should_update_article() {
    loginAs(user);
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { updateArticle(slug: \""
                + article.getSlug()
                + "\", changes: {title: \"new title\", body: \"new body\"}) { article { slug } } }");

    assertThat(context.<String>read("data.updateArticle.article.slug"))
        .isEqualTo(article.getSlug());
    ArgumentCaptor<UpdateArticleParam> captor = ArgumentCaptor.forClass(UpdateArticleParam.class);
    verify(articleCommandService).updateArticle(eq(article), captor.capture());
    assertThat(captor.getValue().getTitle()).isEqualTo("new title");
    assertThat(captor.getValue().getBody()).isEqualTo("new body");
  }

  @Test
  public void should_not_update_article_of_other_user() {
    loginAs(new User("other@test.com", "other", "123", "", ""));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { updateArticle(slug: \""
                + article.getSlug()
                + "\", changes: {title: \"new title\"}) { article { slug } } }");

    assertThat(result.getErrors()).hasSize(1);
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  public void should_favorite_article() {
    loginAs(user);

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { favoriteArticle(slug: \""
                + article.getSlug()
                + "\") { article { slug } } }");

    assertThat(context.<String>read("data.favoriteArticle.article.slug"))
        .isEqualTo(article.getSlug());
    verify(articleFavoriteRepository).save(eq(new ArticleFavorite(article.getId(), user.getId())));
  }

  @Test
  public void should_return_error_when_favoriting_unknown_article() {
    loginAs(user);
    when(articleRepository.findBySlug(eq("unknown"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { favoriteArticle(slug: \"unknown\") { article { slug } } }");

    assertThat(result.getErrors()).hasSize(1);
    verify(articleFavoriteRepository, never()).save(any());
  }

  @Test
  public void should_unfavorite_article() {
    loginAs(user);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { unfavoriteArticle(slug: \""
                + article.getSlug()
                + "\") { article { slug } } }");

    assertThat(context.<String>read("data.unfavoriteArticle.article.slug"))
        .isEqualTo(article.getSlug());
    verify(articleFavoriteRepository).remove(eq(favorite));
  }

  @Test
  public void should_delete_article() {
    loginAs(user);

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { deleteArticle(slug: \"" + article.getSlug() + "\") { success } }",
            "data.deleteArticle.success");

    assertThat(success).isTrue();
    verify(articleRepository).remove(eq(article));
  }

  @Test
  public void should_not_delete_article_of_other_user() {
    loginAs(new User("other@test.com", "other", "123", "", ""));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { deleteArticle(slug: \"" + article.getSlug() + "\") { success } }");

    assertThat(result.getErrors()).hasSize(1);
    verify(articleRepository, never()).remove(any());
  }

  private static Map<String, Object> singletonVariable(String key, Object value) {
    Map<String, Object> variables = new HashMap<>();
    variables.put(key, value);
    return variables;
  }
}
