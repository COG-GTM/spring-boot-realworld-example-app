package io.spring.core.favorite;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_set_article_and_user_ids_on_construction() {
    ArticleFavorite favorite = new ArticleFavorite("articleId", "userId");
    assertThat(favorite.getArticleId()).isEqualTo("articleId");
    assertThat(favorite.getUserId()).isEqualTo("userId");
  }

  @Test
  public void should_be_equal_when_both_ids_match() {
    ArticleFavorite first = new ArticleFavorite("articleId", "userId");
    ArticleFavorite second = new ArticleFavorite("articleId", "userId");
    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
  }

  @Test
  public void should_not_be_equal_when_article_id_differs() {
    ArticleFavorite first = new ArticleFavorite("articleId", "userId");
    ArticleFavorite second = new ArticleFavorite("otherArticle", "userId");
    assertThat(first).isNotEqualTo(second);
  }

  @Test
  public void should_not_be_equal_when_user_id_differs() {
    ArticleFavorite first = new ArticleFavorite("articleId", "userId");
    ArticleFavorite second = new ArticleFavorite("articleId", "otherUser");
    assertThat(first).isNotEqualTo(second);
  }

  @Test
  public void should_allow_null_ids() {
    ArticleFavorite favorite = new ArticleFavorite(null, null);
    assertThat(favorite.getArticleId()).isNull();
    assertThat(favorite.getUserId()).isNull();
  }
}
