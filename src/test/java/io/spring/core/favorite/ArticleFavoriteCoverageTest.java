package io.spring.core.favorite;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteCoverageTest {

  @Test
  public void should_create_article_favorite() {
    ArticleFavorite favorite = new ArticleFavorite("articleId", "userId");
    assertEquals("articleId", favorite.getArticleId());
    assertEquals("userId", favorite.getUserId());
  }

  @Test
  public void should_have_no_arg_constructor() {
    ArticleFavorite favorite = new ArticleFavorite();
    assertNull(favorite.getArticleId());
    assertNull(favorite.getUserId());
  }

  @Test
  public void should_have_equals_and_hashcode() {
    ArticleFavorite fav1 = new ArticleFavorite("article1", "user1");
    ArticleFavorite fav2 = new ArticleFavorite("article1", "user1");
    ArticleFavorite fav3 = new ArticleFavorite("article2", "user1");

    assertEquals(fav1, fav2);
    assertEquals(fav1.hashCode(), fav2.hashCode());
    assertNotEquals(fav1, fav3);
  }

  @Test
  public void should_not_equal_null() {
    ArticleFavorite fav = new ArticleFavorite("article1", "user1");
    assertNotEquals(fav, null);
  }

  @Test
  public void should_not_equal_different_type() {
    ArticleFavorite fav = new ArticleFavorite("article1", "user1");
    assertNotEquals(fav, "string");
  }
}
