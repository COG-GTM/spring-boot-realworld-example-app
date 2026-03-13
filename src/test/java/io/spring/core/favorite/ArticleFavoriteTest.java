package io.spring.core.favorite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_set_fields_via_constructor() {
    ArticleFavorite favorite = new ArticleFavorite("articleId", "userId");
    assertThat(favorite.getArticleId(), is("articleId"));
    assertThat(favorite.getUserId(), is("userId"));
  }

  @Test
  public void should_equal_when_same_fields() {
    ArticleFavorite fav1 = new ArticleFavorite("articleId", "userId");
    ArticleFavorite fav2 = new ArticleFavorite("articleId", "userId");
    assertThat(fav1.equals(fav2), is(true));
    assertThat(fav1.hashCode(), is(fav2.hashCode()));
  }

  @Test
  public void should_not_equal_when_different_fields() {
    ArticleFavorite fav1 = new ArticleFavorite("article1", "user1");
    ArticleFavorite fav2 = new ArticleFavorite("article2", "user2");
    assertThat(fav1.equals(fav2), is(false));
  }
}
