package io.spring.core.favorite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_create_article_favorite_with_article_and_user_id() {
    String articleId = "article123";
    String userId = "user456";

    ArticleFavorite favorite = new ArticleFavorite(articleId, userId);

    assertThat(favorite.getArticleId(), is(articleId));
    assertThat(favorite.getUserId(), is(userId));
  }

  @Test
  public void should_have_equality_based_on_both_ids() {
    ArticleFavorite favorite1 = new ArticleFavorite("article1", "user1");
    ArticleFavorite favorite2 = new ArticleFavorite("article1", "user1");
    ArticleFavorite favorite3 = new ArticleFavorite("article1", "user2");
    ArticleFavorite favorite4 = new ArticleFavorite("article2", "user1");

    assertThat(favorite1, is(favorite2));
    assertThat(favorite1.hashCode(), is(favorite2.hashCode()));
    assertThat(favorite1, not(favorite3));
    assertThat(favorite1, not(favorite4));
  }
}
