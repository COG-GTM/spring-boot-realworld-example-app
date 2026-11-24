package io.spring.core.favorite;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_be_equal_when_article_and_user_match() {
    ArticleFavorite a = new ArticleFavorite("article", "user");
    ArticleFavorite b = new ArticleFavorite("article", "user");

    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a.getArticleId()).isEqualTo("article");
    assertThat(a.getUserId()).isEqualTo("user");
  }

  @Test
  public void should_not_be_equal_for_different_user() {
    assertThat(new ArticleFavorite("article", "user"))
        .isNotEqualTo(new ArticleFavorite("article", "other"));
  }
}
