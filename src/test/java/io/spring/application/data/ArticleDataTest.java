package io.spring.application.data;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.DateTimeCursor;
import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

class ArticleDataTest {

  private ProfileData author() {
    return new ProfileData("aid", "jake", "bio", "img", false);
  }

  private ArticleData fullArticle() {
    return new ArticleData(
        "id",
        "slug",
        "title",
        "desc",
        "body",
        true,
        5,
        new DateTime(1000L),
        new DateTime(2000L),
        Arrays.asList("java", "spring"),
        author());
  }

  @Test
  void allArgsConstructorAndGettersExposeEveryField() {
    ArticleData data = fullArticle();

    assertThat(data.getId()).isEqualTo("id");
    assertThat(data.getSlug()).isEqualTo("slug");
    assertThat(data.getTitle()).isEqualTo("title");
    assertThat(data.getDescription()).isEqualTo("desc");
    assertThat(data.getBody()).isEqualTo("body");
    assertThat(data.isFavorited()).isTrue();
    assertThat(data.getFavoritesCount()).isEqualTo(5);
    assertThat(data.getCreatedAt()).isEqualTo(new DateTime(1000L));
    assertThat(data.getUpdatedAt()).isEqualTo(new DateTime(2000L));
    assertThat(data.getTagList()).containsExactly("java", "spring");
    assertThat(data.getProfileData()).isEqualTo(author());
  }

  @Test
  void noArgsConstructorAndSettersMutateState() {
    ArticleData data = new ArticleData();

    data.setId("id2");
    data.setSlug("slug2");
    data.setTitle("title2");
    data.setDescription("desc2");
    data.setBody("body2");
    data.setFavorited(false);
    data.setFavoritesCount(0);
    DateTime created = new DateTime(3000L);
    DateTime updated = new DateTime(4000L);
    data.setCreatedAt(created);
    data.setUpdatedAt(updated);
    data.setTagList(Collections.emptyList());
    ProfileData profile = author();
    data.setProfileData(profile);

    assertThat(data.getId()).isEqualTo("id2");
    assertThat(data.getSlug()).isEqualTo("slug2");
    assertThat(data.getTitle()).isEqualTo("title2");
    assertThat(data.getDescription()).isEqualTo("desc2");
    assertThat(data.getBody()).isEqualTo("body2");
    assertThat(data.isFavorited()).isFalse();
    assertThat(data.getFavoritesCount()).isZero();
    assertThat(data.getCreatedAt()).isEqualTo(created);
    assertThat(data.getUpdatedAt()).isEqualTo(updated);
    assertThat(data.getTagList()).isEmpty();
    assertThat(data.getProfileData()).isSameAs(profile);
  }

  @Test
  void getCursorIsDerivedFromUpdatedAt() {
    ArticleData data = fullArticle();

    DateTimeCursor cursor = data.getCursor();

    assertThat(cursor).isNotNull();
    assertThat(cursor.getData()).isEqualTo(new DateTime(2000L));
    assertThat(cursor.toString()).isEqualTo("2000");
  }

  @Test
  void getCursorHandlesNullUpdatedAtField() {
    ArticleData data = new ArticleData();

    DateTimeCursor cursor = data.getCursor();

    assertThat(cursor.getData()).isNull();
  }

  @Test
  void equalsAndHashCodeHoldForIdenticalContent() {
    ArticleData a = fullArticle();
    ArticleData b = fullArticle();

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    assertThat(a).isEqualTo(a);
    assertThat(a).isNotEqualTo(null).isNotEqualTo("string");
  }

  @Test
  void equalsDistinguishesEveryField() {
    ArticleData base = fullArticle();

    assertThat(base).isNotEqualTo(withId(base, "other"));
    assertThat(base).isNotEqualTo(mutate(d -> d.setSlug("x")));
    assertThat(base).isNotEqualTo(mutate(d -> d.setTitle("x")));
    assertThat(base).isNotEqualTo(mutate(d -> d.setDescription("x")));
    assertThat(base).isNotEqualTo(mutate(d -> d.setBody("x")));
    assertThat(base).isNotEqualTo(mutate(d -> d.setFavorited(false)));
    assertThat(base).isNotEqualTo(mutate(d -> d.setFavoritesCount(99)));
    assertThat(base).isNotEqualTo(mutate(d -> d.setCreatedAt(new DateTime(0L))));
    assertThat(base).isNotEqualTo(mutate(d -> d.setUpdatedAt(new DateTime(0L))));
    assertThat(base).isNotEqualTo(mutate(d -> d.setTagList(Collections.emptyList())));
    assertThat(base).isNotEqualTo(mutate(d -> d.setProfileData(null)));
  }

  @Test
  void toStringContainsKeyFields() {
    String text = fullArticle().toString();

    assertThat(text).contains("ArticleData", "slug=slug", "title=title", "favoritesCount=5");
  }

  private ArticleData withId(ArticleData base, String id) {
    ArticleData copy = fullArticle();
    copy.setId(id);
    return copy;
  }

  private ArticleData mutate(java.util.function.Consumer<ArticleData> change) {
    ArticleData copy = fullArticle();
    change.accept(copy);
    return copy;
  }
}
