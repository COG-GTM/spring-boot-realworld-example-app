package io.spring.core.favorite;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_create_article_favorite() {
    ArticleFavorite fav = new ArticleFavorite("article1", "user1");
    assertEquals("article1", fav.getArticleId());
    assertEquals("user1", fav.getUserId());
  }

  @Test
  public void should_equal_with_same_fields() {
    ArticleFavorite fav1 = new ArticleFavorite("article1", "user1");
    ArticleFavorite fav2 = new ArticleFavorite("article1", "user1");
    assertEquals(fav1, fav2);
  }

  @Test
  public void should_not_equal_with_different_article() {
    ArticleFavorite fav1 = new ArticleFavorite("article1", "user1");
    ArticleFavorite fav2 = new ArticleFavorite("article2", "user1");
    assertNotEquals(fav1, fav2);
  }

  @Test
  public void should_not_equal_with_different_user() {
    ArticleFavorite fav1 = new ArticleFavorite("article1", "user1");
    ArticleFavorite fav2 = new ArticleFavorite("article1", "user2");
    assertNotEquals(fav1, fav2);
  }

  @Test
  public void should_have_same_hashcode_for_equal_objects() {
    ArticleFavorite fav1 = new ArticleFavorite("article1", "user1");
    ArticleFavorite fav2 = new ArticleFavorite("article1", "user1");
    assertEquals(fav1.hashCode(), fav2.hashCode());
  }

  @Test
  public void should_create_with_no_arg_constructor() {
    ArticleFavorite fav = new ArticleFavorite();
    assertNull(fav.getArticleId());
    assertNull(fav.getUserId());
  }
}
