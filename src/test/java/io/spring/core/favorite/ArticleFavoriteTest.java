package io.spring.core.favorite;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  void should_create_article_favorite() {
    ArticleFavorite favorite = new ArticleFavorite("articleId1", "userId1");
    assertEquals("articleId1", favorite.getArticleId());
    assertEquals("userId1", favorite.getUserId());
  }

  @Test
  void should_support_no_arg_constructor() {
    ArticleFavorite favorite = new ArticleFavorite();
    assertNull(favorite.getArticleId());
    assertNull(favorite.getUserId());
  }

  @Test
  void should_have_equality() {
    ArticleFavorite fav1 = new ArticleFavorite("articleId1", "userId1");
    ArticleFavorite fav2 = new ArticleFavorite("articleId1", "userId1");
    assertEquals(fav1, fav2);
  }

  @Test
  void should_not_equal_different_favorite() {
    ArticleFavorite fav1 = new ArticleFavorite("articleId1", "userId1");
    ArticleFavorite fav2 = new ArticleFavorite("articleId2", "userId1");
    assertNotEquals(fav1, fav2);
  }

  @Test
  void should_have_consistent_hashcode() {
    ArticleFavorite fav1 = new ArticleFavorite("articleId1", "userId1");
    ArticleFavorite fav2 = new ArticleFavorite("articleId1", "userId1");
    assertEquals(fav1.hashCode(), fav2.hashCode());
  }
}
