package io.spring.application.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.spring.application.DateTimeCursor;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

class CommentDataTest {

  private ProfileData author() {
    return new ProfileData("aid", "jake", "bio", "img", false);
  }

  private CommentData full() {
    return new CommentData(
        "id", "body", "articleId", new DateTime(1000L), new DateTime(2000L), author());
  }

  @Test
  void allArgsConstructorAndGettersExposeEveryField() {
    CommentData data = full();

    assertThat(data.getId()).isEqualTo("id");
    assertThat(data.getBody()).isEqualTo("body");
    assertThat(data.getArticleId()).isEqualTo("articleId");
    assertThat(data.getCreatedAt()).isEqualTo(new DateTime(1000L));
    assertThat(data.getUpdatedAt()).isEqualTo(new DateTime(2000L));
    assertThat(data.getProfileData()).isEqualTo(author());
  }

  @Test
  void noArgsConstructorAndSettersMutateState() {
    CommentData data = new CommentData();

    data.setId("id2");
    data.setBody("body2");
    data.setArticleId("aid2");
    DateTime created = new DateTime(3000L);
    DateTime updated = new DateTime(4000L);
    data.setCreatedAt(created);
    data.setUpdatedAt(updated);
    ProfileData profile = author();
    data.setProfileData(profile);

    assertThat(data.getId()).isEqualTo("id2");
    assertThat(data.getBody()).isEqualTo("body2");
    assertThat(data.getArticleId()).isEqualTo("aid2");
    assertThat(data.getCreatedAt()).isEqualTo(created);
    assertThat(data.getUpdatedAt()).isEqualTo(updated);
    assertThat(data.getProfileData()).isSameAs(profile);
  }

  @Test
  void getCursorIsDerivedFromCreatedAt() {
    CommentData data = full();

    DateTimeCursor cursor = data.getCursor();

    assertThat(cursor).isNotNull();
    assertThat(cursor.getData()).isEqualTo(new DateTime(1000L));
    assertThat(cursor.toString()).isEqualTo("1000");
  }

  @Test
  void getCursorWrapsNullWhenCreatedAtIsNull() {
    CommentData data = new CommentData();

    DateTimeCursor cursor = data.getCursor();

    assertThat(cursor).isNotNull();
    assertThat(cursor.getData()).isNull();
    assertThatThrownBy(cursor::toString).isInstanceOf(NullPointerException.class);
  }

  @Test
  void equalsAndHashCodeHoldForIdenticalContent() {
    CommentData a = full();
    CommentData b = full();

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    assertThat(a).isEqualTo(a);
    assertThat(a).isNotEqualTo(null).isNotEqualTo("string");
  }

  @Test
  void equalsDistinguishesEveryField() {
    CommentData base = full();

    assertThat(base).isNotEqualTo(mutate(d -> d.setId("x")));
    assertThat(base).isNotEqualTo(mutate(d -> d.setBody("x")));
    assertThat(base).isNotEqualTo(mutate(d -> d.setArticleId("x")));
    assertThat(base).isNotEqualTo(mutate(d -> d.setCreatedAt(new DateTime(0L))));
    assertThat(base).isNotEqualTo(mutate(d -> d.setUpdatedAt(new DateTime(0L))));
    assertThat(base).isNotEqualTo(mutate(d -> d.setProfileData(null)));
  }

  @Test
  void toStringContainsKeyFields() {
    assertThat(full().toString()).contains("CommentData", "body=body", "articleId=articleId");
  }

  private CommentData mutate(java.util.function.Consumer<CommentData> change) {
    CommentData copy = full();
    change.accept(copy);
    return copy;
  }
}
