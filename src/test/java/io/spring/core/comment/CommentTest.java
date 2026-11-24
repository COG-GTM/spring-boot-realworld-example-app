package io.spring.core.comment;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_create_comment_with_generated_id_and_timestamp() {
    DateTime before = new DateTime();
    Comment comment = new Comment("body", "user", "article");

    assertThat(comment.getId()).isNotEmpty();
    assertThat(comment.getBody()).isEqualTo("body");
    assertThat(comment.getUserId()).isEqualTo("user");
    assertThat(comment.getArticleId()).isEqualTo("article");
    assertThat(comment.getCreatedAt()).isNotNull();
    assertThat(comment.getCreatedAt().isBefore(before)).isFalse();
  }

  @Test
  public void should_compare_comments_by_id() {
    Comment a = new Comment("body", "user", "article");
    Comment b = new Comment("body", "user", "article");

    assertThat(a).isNotEqualTo(b);
    assertThat(a).isEqualTo(a);
  }
}
