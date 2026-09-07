package io.spring.application.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class ArticleCommandServiceTest {

  @Test
  public void should_create_article() {
    ArticleRepository articleRepository = mock(ArticleRepository.class);
    ArticleCommandService service = new ArticleCommandService(articleRepository);
    User creator = new User("email", "username", "password", "", "");

    Article article =
        service.createArticle(
            new NewArticleParam("Title", "Description", "Body", Arrays.asList("java")), creator);

    assertThat(article.getTitle(), is("Title"));
    assertThat(article.getDescription(), is("Description"));
    assertThat(article.getBody(), is("Body"));
    assertThat(article.getUserId(), is(creator.getId()));
    verify(articleRepository).save(article);
  }

  @Test
  public void should_update_article() {
    ArticleRepository articleRepository = mock(ArticleRepository.class);
    ArticleCommandService service = new ArticleCommandService(articleRepository);
    Article article =
        new Article("Old title", "Old description", "Old body", Arrays.asList("java"), "user-id");

    Article updated =
        service.updateArticle(
            article, new UpdateArticleParam("New title", "New body", "New description"));

    assertThat(updated, is(article));
    assertThat(article.getTitle(), is("New title"));
    assertThat(article.getDescription(), is("New description"));
    assertThat(article.getBody(), is("New body"));
    verify(articleRepository).save(article);
  }
}
