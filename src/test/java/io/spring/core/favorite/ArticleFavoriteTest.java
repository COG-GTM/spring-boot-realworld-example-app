package io.spring.core.favorite;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArticleFavoriteTest {

  @Test
  void should_hold_article_and_user_ids() {
    ArticleFavorite favorite = new ArticleFavorite("articleId", "userId");

    assertThat(favorite.getArticleId()).isEqualTo("articleId");
    assertThat(favorite.getUserId()).isEqualTo("userId");
  }

  @Test
  void should_leave_ids_null_with_no_args_constructor() {
    ArticleFavorite favorite = new ArticleFavorite();

    assertThat(favorite.getArticleId()).isNull();
    assertThat(favorite.getUserId()).isNull();
  }

  @Test
  void should_compare_by_both_ids() {
    ArticleFavorite favorite = new ArticleFavorite("articleId", "userId");
    ArticleFavorite same = new ArticleFavorite("articleId", "userId");

    assertThat(favorite).isEqualTo(favorite).isEqualTo(same).hasSameHashCodeAs(same);
    assertThat(favorite).isNotEqualTo(new ArticleFavorite("other", "userId"));
    assertThat(favorite).isNotEqualTo(new ArticleFavorite("articleId", "other"));
    assertThat(favorite).isNotEqualTo(new ArticleFavorite());
    assertThat(favorite).isNotEqualTo(null).isNotEqualTo("favorite");
  }
}
