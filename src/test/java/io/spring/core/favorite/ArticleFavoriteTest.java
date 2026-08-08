package io.spring.core.favorite;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_keep_article_and_user_id() {
    ArticleFavorite favorite = new ArticleFavorite("article-id", "user-id");

    assertThat(favorite.getArticleId()).isEqualTo("article-id");
    assertThat(favorite.getUserId()).isEqualTo("user-id");
  }

  @Test
  public void should_use_all_fields_for_equality() {
    ArticleFavorite favorite = new ArticleFavorite("article-id", "user-id");

    assertThat(favorite).isEqualTo(new ArticleFavorite("article-id", "user-id"));
    assertThat(favorite.hashCode())
        .isEqualTo(new ArticleFavorite("article-id", "user-id").hashCode());
    assertThat(favorite).isNotEqualTo(new ArticleFavorite("article-id", "other-user"));
    assertThat(favorite).isNotEqualTo(new ArticleFavorite("other-article", "user-id"));
  }

  @Test
  public void should_support_no_args_constructor() {
    ArticleFavorite favorite = new ArticleFavorite();

    assertThat(favorite.getArticleId()).isNull();
    assertThat(favorite.getUserId()).isNull();
    assertThat(favorite).isEqualTo(new ArticleFavorite());
  }
}
