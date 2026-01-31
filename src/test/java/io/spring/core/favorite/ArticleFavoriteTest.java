package io.spring.core.favorite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_create_article_favorite_with_article_id() {
    ArticleFavorite favorite = new ArticleFavorite("article123", "user123");
    assertThat(favorite.getArticleId(), is("article123"));
  }

  @Test
  public void should_create_article_favorite_with_user_id() {
    ArticleFavorite favorite = new ArticleFavorite("article123", "user123");
    assertThat(favorite.getUserId(), is("user123"));
  }

  @Test
  public void should_equal_when_same_article_and_user() {
    ArticleFavorite favorite1 = new ArticleFavorite("article123", "user123");
    ArticleFavorite favorite2 = new ArticleFavorite("article123", "user123");
    assertThat(favorite1, is(favorite2));
  }

  @Test
  public void should_not_equal_when_different_article() {
    ArticleFavorite favorite1 = new ArticleFavorite("article123", "user123");
    ArticleFavorite favorite2 = new ArticleFavorite("article456", "user123");
    assertThat(favorite1, is(not(favorite2)));
  }

  @Test
  public void should_not_equal_when_different_user() {
    ArticleFavorite favorite1 = new ArticleFavorite("article123", "user123");
    ArticleFavorite favorite2 = new ArticleFavorite("article123", "user456");
    assertThat(favorite1, is(not(favorite2)));
  }
}
