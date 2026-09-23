package io.spring.application.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleCommandServiceTest {

  @Mock private ArticleRepository articleRepository;

  private ArticleCommandService articleCommandService;
  private final User creator = new User("email@test.com", "username", "123", "", "");

  @BeforeEach
  void setUp() {
    articleCommandService = new ArticleCommandService(articleRepository);
  }

  @Test
  public void should_create_article_for_creator_and_save_it() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("a new title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertThat(article.getTitle(), is("a new title"));
    assertThat(article.getSlug(), is("a-new-title"));
    assertThat(article.getUserId(), is(creator.getId()));
    assertThat(article.getTags().size(), is(2));
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_update_article_and_save_it() {
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), creator.getId());

    Article updated =
        articleCommandService.updateArticle(
            article, new UpdateArticleParam("new title", "new body", ""));

    assertThat(updated.getTitle(), is("new title"));
    assertThat(updated.getSlug(), is("new-title"));
    assertThat(updated.getBody(), is("new body"));
    assertThat(updated.getDescription(), is("old desc"));
    verify(articleRepository).save(article);
  }
}
