package io.spring.application.article;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ArticleCommandServiceTest {

  @Mock private ArticleRepository articleRepository;

  private ArticleCommandService articleCommandService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    articleCommandService = new ArticleCommandService(articleRepository);
  }

  @Test
  public void should_create_article_and_save() {
    User creator = new User("email@test.com", "username", "pass", "", "");
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
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
    assertThat(article.getUserId(), is(creator.getId()));
    List<String> tagNames =
        article.getTags().stream().map(Tag::getName).collect(Collectors.toList());
    assertThat(tagNames, hasItems("java", "spring"));
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_update_article_content_and_save() {
    User creator = new User("email@test.com", "username", "pass", "", "");
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), creator.getId());

    Article updated =
        articleCommandService.updateArticle(
            article, new UpdateArticleParam("new title", "new body", "new desc"));

    assertThat(updated.getTitle(), is("new title"));
    assertThat(updated.getSlug(), is("new-title"));
    assertThat(updated.getBody(), is("new body"));
    assertThat(updated.getDescription(), is("new desc"));
    verify(articleRepository).save(article);
  }
}
