package io.spring.core.favorite;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  void should_set_fields_via_constructor() {
    ArticleFavorite fav = new ArticleFavorite("article1", "user1");
    assertEquals("article1", fav.getArticleId());
    assertEquals("user1", fav.getUserId());
  }

  @Test
  void should_be_equal_when_same_articleId_and_userId() {
    ArticleFavorite fav1 = new ArticleFavorite("article1", "user1");
    ArticleFavorite fav2 = new ArticleFavorite("article1", "user1");
    assertEquals(fav1, fav2);
    assertEquals(fav1.hashCode(), fav2.hashCode());
  }

  @Test
  void should_not_be_equal_when_different_fields() {
    ArticleFavorite fav1 = new ArticleFavorite("article1", "user1");
    ArticleFavorite fav2 = new ArticleFavorite("article2", "user1");
    assertNotEquals(fav1, fav2);
  }
}
