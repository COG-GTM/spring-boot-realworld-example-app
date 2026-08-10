package io.spring.core.comment;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

class CommentTest {

  @Test
  void should_create_comment_with_generated_id_and_creation_time() {
    DateTime before = new DateTime().minusSeconds(1);

    Comment comment = new Comment("body", "userId", "articleId");

    assertThat(comment.getId()).isNotBlank();
    assertThat(comment.getBody()).isEqualTo("body");
    assertThat(comment.getUserId()).isEqualTo("userId");
    assertThat(comment.getArticleId()).isEqualTo("articleId");
    assertThat(comment.getCreatedAt().isAfter(before)).isTrue();
  }

  @Test
  void should_generate_unique_id_per_comment() {
    Comment one = new Comment("body", "userId", "articleId");
    Comment another = new Comment("body", "userId", "articleId");

    assertThat(one.getId()).isNotEqualTo(another.getId());
  }

  @Test
  void should_leave_fields_null_with_no_args_constructor() {
    Comment comment = new Comment();

    assertThat(comment.getId()).isNull();
    assertThat(comment.getBody()).isNull();
    assertThat(comment.getCreatedAt()).isNull();
  }

  @Test
  void should_compare_comments_by_id_only() {
    Comment comment = new Comment("body", "userId", "articleId");
    Comment other = new Comment("body", "userId", "articleId");

    assertThat(comment).isEqualTo(comment).hasSameHashCodeAs(comment);
    assertThat(comment).isNotEqualTo(other);
    assertThat(comment).isNotEqualTo(null).isNotEqualTo("comment");
    assertThat(comment).isNotEqualTo(new Comment());
  }
}
