package io.spring.core.favorite;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_create_article_favorite() {
    ArticleFavorite fav = new ArticleFavorite("article123", "user456");
    assertEquals("article123", fav.getArticleId());
    assertEquals("user456", fav.getUserId());
  }

  @Test
  public void should_have_equality_based_on_all_fields() {
    ArticleFavorite f1 = new ArticleFavorite("article1", "user1");
    ArticleFavorite f2 = new ArticleFavorite("article1", "user1");
    assertEquals(f1, f2);
  }

  @Test
  public void should_not_be_equal_with_different_article_id() {
    ArticleFavorite f1 = new ArticleFavorite("article1", "user1");
    ArticleFavorite f2 = new ArticleFavorite("article2", "user1");
    assertNotEquals(f1, f2);
  }

  @Test
  public void should_not_be_equal_with_different_user_id() {
    ArticleFavorite f1 = new ArticleFavorite("article1", "user1");
    ArticleFavorite f2 = new ArticleFavorite("article1", "user2");
    assertNotEquals(f1, f2);
  }
}
