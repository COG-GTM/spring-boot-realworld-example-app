package io.spring.core.comment;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_populate_fields_on_creation() {
    DateTime before = new DateTime().minusSeconds(1);
    Comment comment = new Comment("content", "user-id", "article-id");

    assertThat(comment.getId()).isNotBlank();
    assertThat(comment.getBody()).isEqualTo("content");
    assertThat(comment.getUserId()).isEqualTo("user-id");
    assertThat(comment.getArticleId()).isEqualTo("article-id");
    DateTime after = new DateTime().plusSeconds(1);
    assertThat(comment.getCreatedAt().isAfter(before)).isTrue();
    assertThat(comment.getCreatedAt().isBefore(after)).isTrue();
  }

  @Test
  public void should_use_id_for_equality() {
    Comment one = new Comment("content", "user-id", "article-id");
    Comment other = new Comment("other content", "other-user", "other-article");
    Comment empty = new Comment();

    assertThat(one).isNotEqualTo(other);
    assertThat(one).isEqualTo(one);
    assertThat(one.hashCode()).isEqualTo(one.hashCode());
    assertThat(empty.getId()).isNull();
    assertThat(empty).isNotEqualTo(one);
  }
}
