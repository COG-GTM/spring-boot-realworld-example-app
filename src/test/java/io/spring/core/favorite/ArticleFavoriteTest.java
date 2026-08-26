package io.spring.core.favorite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_keep_article_and_user() {
    ArticleFavorite favorite = new ArticleFavorite("article", "user");
    assertThat(favorite.getArticleId(), is("article"));
    assertThat(favorite.getUserId(), is("user"));
  }

  @Test
  public void should_be_equal_with_same_article_and_user() {
    assertThat(new ArticleFavorite("article", "user"), is(new ArticleFavorite("article", "user")));
    assertThat(
        new ArticleFavorite("article", "user").hashCode(),
        is(new ArticleFavorite("article", "user").hashCode()));
  }

  @Test
  public void should_not_be_equal_when_user_differs() {
    assertThat(
        new ArticleFavorite("article", "user"), not(new ArticleFavorite("article", "other")));
  }
}
