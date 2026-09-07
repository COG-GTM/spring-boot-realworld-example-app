package io.spring.core.favorite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_get_article_and_user_ids() {
    ArticleFavorite favorite = new ArticleFavorite("article-id", "user-id");

    assertThat(favorite.getArticleId(), is("article-id"));
    assertThat(favorite.getUserId(), is("user-id"));
  }

  @Test
  public void should_be_equal_when_article_and_user_are_the_same() {
    ArticleFavorite first = new ArticleFavorite("article-id", "user-id");
    ArticleFavorite second = new ArticleFavorite("article-id", "user-id");

    assertThat(first.equals(second), is(true));
    assertThat(first.hashCode(), is(second.hashCode()));
  }

  @Test
  public void should_not_be_equal_when_article_or_user_differs() {
    ArticleFavorite favorite = new ArticleFavorite("article-id", "user-id");

    assertThat(favorite.equals(new ArticleFavorite("other-article", "user-id")), is(false));
    assertThat(favorite.equals(new ArticleFavorite("article-id", "other-user")), is(false));
  }
}
