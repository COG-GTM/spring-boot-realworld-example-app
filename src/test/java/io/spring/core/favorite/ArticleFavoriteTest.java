package io.spring.core.favorite;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_keep_both_ids_from_constructor() {
    ArticleFavorite favorite = new ArticleFavorite("articleId", "userId");

    assertThat(favorite.getArticleId()).isEqualTo("articleId");
    assertThat(favorite.getUserId()).isEqualTo("userId");
  }

  @Test
  public void should_leave_both_ids_null_with_no_args_constructor() {
    ArticleFavorite favorite = new ArticleFavorite();

    assertThat(favorite.getArticleId()).isNull();
    assertThat(favorite.getUserId()).isNull();
  }

  @Test
  public void should_use_all_fields_for_equals_and_hashcode() {
    ArticleFavorite favorite = new ArticleFavorite("articleId", "userId");
    ArticleFavorite same = new ArticleFavorite("articleId", "userId");

    assertThat(same).isEqualTo(favorite);
    assertThat(same.hashCode()).isEqualTo(favorite.hashCode());
  }

  @Test
  public void should_not_be_equal_when_any_field_differs() {
    ArticleFavorite favorite = new ArticleFavorite("articleId", "userId");

    assertThat(new ArticleFavorite("otherArticleId", "userId")).isNotEqualTo(favorite);
    assertThat(new ArticleFavorite("articleId", "otherUserId")).isNotEqualTo(favorite);
    assertThat(new ArticleFavorite()).isNotEqualTo(favorite);
  }

  @Test
  public void should_not_be_equal_to_null_or_other_types() {
    ArticleFavorite favorite = new ArticleFavorite("articleId", "userId");

    assertThat(favorite).isNotEqualTo(null);
    assertThat(favorite).isNotEqualTo("articleId");
    assertThat(favorite).isEqualTo(favorite);
  }

  @Test
  public void should_be_equal_when_both_favorites_are_empty() {
    assertThat(new ArticleFavorite()).isEqualTo(new ArticleFavorite());
    assertThat(new ArticleFavorite().hashCode()).isEqualTo(new ArticleFavorite().hashCode());
  }
}
