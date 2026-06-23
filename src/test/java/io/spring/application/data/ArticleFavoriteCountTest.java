package io.spring.application.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArticleFavoriteCountTest {

  @Test
  void constructorAndGettersExposeEveryField() {
    ArticleFavoriteCount count = new ArticleFavoriteCount("article-id", 7);

    assertThat(count.getId()).isEqualTo("article-id");
    assertThat(count.getCount()).isEqualTo(7);
  }

  @Test
  void supportsNullAndZeroValues() {
    ArticleFavoriteCount count = new ArticleFavoriteCount(null, 0);

    assertThat(count.getId()).isNull();
    assertThat(count.getCount()).isZero();
  }

  @Test
  void equalsAndHashCodeHoldForIdenticalContent() {
    ArticleFavoriteCount a = new ArticleFavoriteCount("id", 3);
    ArticleFavoriteCount b = new ArticleFavoriteCount("id", 3);

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    assertThat(a).isEqualTo(a);
    assertThat(a).isNotEqualTo(null).isNotEqualTo("string");
  }

  @Test
  void equalsDistinguishesEachField() {
    ArticleFavoriteCount base = new ArticleFavoriteCount("id", 3);

    assertThat(base).isNotEqualTo(new ArticleFavoriteCount("other", 3));
    assertThat(base).isNotEqualTo(new ArticleFavoriteCount("id", 4));
    assertThat(base).isNotEqualTo(new ArticleFavoriteCount(null, 3));
    assertThat(base).isNotEqualTo(new ArticleFavoriteCount("id", null));
  }

  @Test
  void toStringContainsBothFields() {
    assertThat(new ArticleFavoriteCount("id", 3).toString())
        .contains("ArticleFavoriteCount", "id=id", "count=3");
  }
}
