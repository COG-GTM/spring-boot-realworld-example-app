package io.spring.core.favorite;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_assign_article_id_and_user_id() {
    ArticleFavorite favorite = new ArticleFavorite("article-id", "user-id");

    assertThat(favorite.getArticleId()).isEqualTo("article-id");
    assertThat(favorite.getUserId()).isEqualTo("user-id");
  }

  @Test
  public void should_be_equal_when_both_ids_match() {
    ArticleFavorite favorite = new ArticleFavorite("article-id", "user-id");
    ArticleFavorite same = new ArticleFavorite("article-id", "user-id");

    assertThat(favorite).isEqualTo(same);
    assertThat(favorite.hashCode()).isEqualTo(same.hashCode());
  }

  @Test
  public void should_not_be_equal_when_ids_are_swapped() {
    ArticleFavorite favorite = new ArticleFavorite("article-id", "user-id");

    assertThat(favorite).isNotEqualTo(new ArticleFavorite("user-id", "article-id"));
    assertThat(favorite).isNotEqualTo(new ArticleFavorite("article-id", "other-user-id"));
  }

  @Test
  public void should_not_be_equal_to_null_or_other_types() {
    ArticleFavorite favorite = new ArticleFavorite("article-id", "user-id");

    assertThat(favorite).isNotEqualTo(null);
    assertThat(favorite).isNotEqualTo("not a favorite");
  }

  @Test
  public void should_create_empty_favorite_with_no_args_constructor() {
    ArticleFavorite favorite = new ArticleFavorite();

    assertThat(favorite.getArticleId()).isNull();
    assertThat(favorite.getUserId()).isNull();
  }
}
