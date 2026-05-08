package io.spring.core.favorite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_store_article_id_and_user_id() {
    ArticleFavorite favorite = new ArticleFavorite("article-1", "user-1");

    assertEquals("article-1", favorite.getArticleId());
    assertEquals("user-1", favorite.getUserId());
  }

  @Test
  public void favorites_with_same_ids_should_be_equal() {
    ArticleFavorite a = new ArticleFavorite("article-1", "user-1");
    ArticleFavorite b = new ArticleFavorite("article-1", "user-1");
    assertEquals(a, b);
  }
}
