package io.spring.core.favorite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_create_article_favorite() {
    ArticleFavorite favorite = new ArticleFavorite("article-id", "user-id");

    assertThat(favorite.getArticleId(), is("article-id"));
    assertThat(favorite.getUserId(), is("user-id"));
  }

  @Test
  public void should_create_empty_article_favorite() {
    ArticleFavorite favorite = new ArticleFavorite();

    assertThat(favorite.getArticleId(), is((String) null));
    assertThat(favorite.getUserId(), is((String) null));
  }

  @Test
  public void should_implement_equals_and_hashcode() {
    ArticleFavorite favorite1 = new ArticleFavorite("article-id", "user-id");
    ArticleFavorite favorite2 = new ArticleFavorite("article-id", "user-id");
    ArticleFavorite favorite3 = new ArticleFavorite("other-article", "other-user");

    assertEquals(favorite1, favorite2);
    assertNotEquals(favorite1, favorite3);
    assertEquals(favorite1.hashCode(), favorite2.hashCode());
    assertThat(favorite1.hashCode(), not(favorite3.hashCode()));
  }
}
