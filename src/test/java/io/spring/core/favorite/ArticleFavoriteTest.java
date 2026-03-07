package io.spring.core.favorite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_create_article_favorite() {
    ArticleFavorite favorite = new ArticleFavorite("article-1", "user-1");

    assertThat(favorite.getArticleId(), is("article-1"));
    assertThat(favorite.getUserId(), is("user-1"));
  }

  @Test
  public void should_have_equality_based_on_all_fields() {
    ArticleFavorite fav1 = new ArticleFavorite("article-1", "user-1");
    ArticleFavorite fav2 = new ArticleFavorite("article-1", "user-1");

    assertThat(fav1.equals(fav2), is(true));
    assertThat(fav1.hashCode(), is(fav2.hashCode()));
  }

  @Test
  public void should_not_be_equal_with_different_article_id() {
    ArticleFavorite fav1 = new ArticleFavorite("article-1", "user-1");
    ArticleFavorite fav2 = new ArticleFavorite("article-2", "user-1");

    assertThat(fav1.equals(fav2), is(false));
  }

  @Test
  public void should_not_be_equal_with_different_user_id() {
    ArticleFavorite fav1 = new ArticleFavorite("article-1", "user-1");
    ArticleFavorite fav2 = new ArticleFavorite("article-1", "user-2");

    assertThat(fav1.equals(fav2), is(false));
  }
}
