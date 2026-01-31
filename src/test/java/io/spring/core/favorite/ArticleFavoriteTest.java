package io.spring.core.favorite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_create_article_favorite_with_all_fields() {
    ArticleFavorite favorite = new ArticleFavorite("article123", "user456");

    assertThat(favorite.getArticleId(), is("article123"));
    assertThat(favorite.getUserId(), is("user456"));
  }

  @Test
  public void should_have_equal_favorites_with_same_article_and_user() {
    ArticleFavorite favorite1 = new ArticleFavorite("article123", "user456");
    ArticleFavorite favorite2 = new ArticleFavorite("article123", "user456");

    assertThat(favorite1.equals(favorite2), is(true));
    assertThat(favorite1.hashCode(), is(favorite2.hashCode()));
  }

  @Test
  public void should_have_different_favorites_with_different_article() {
    ArticleFavorite favorite1 = new ArticleFavorite("article123", "user456");
    ArticleFavorite favorite2 = new ArticleFavorite("article789", "user456");

    assertThat(favorite1.equals(favorite2), is(false));
  }

  @Test
  public void should_have_different_favorites_with_different_user() {
    ArticleFavorite favorite1 = new ArticleFavorite("article123", "user456");
    ArticleFavorite favorite2 = new ArticleFavorite("article123", "user789");

    assertThat(favorite1.equals(favorite2), is(false));
  }

  @Test
  public void should_create_favorite_with_empty_ids() {
    ArticleFavorite favorite = new ArticleFavorite("", "");

    assertThat(favorite.getArticleId(), is(""));
    assertThat(favorite.getUserId(), is(""));
  }
}
