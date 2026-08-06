package io.spring.core.comment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_assign_uuid_id_and_all_fields() {
    DateTime before = new DateTime().minusSeconds(1);
    Comment comment = new Comment("comment body", "user-id", "article-id");

    assertThat(UUID.fromString(comment.getId())).isNotNull();
    assertThat(comment.getBody()).isEqualTo("comment body");
    assertThat(comment.getUserId()).isEqualTo("user-id");
    assertThat(comment.getArticleId()).isEqualTo("article-id");
    assertThat(comment.getCreatedAt()).isNotNull();
    assertThat(comment.getCreatedAt().isAfter(before)).isTrue();
  }

  @Test
  public void should_assign_different_ids_to_different_comments() {
    Comment comment = new Comment("body", "user-id", "article-id");
    Comment other = new Comment("body", "user-id", "article-id");

    assertThat(comment.getId()).isNotEqualTo(other.getId());
  }

  @Test
  public void should_use_id_only_for_equality() {
    Comment comment = new Comment("body", "user-id", "article-id");
    Comment sameFieldsDifferentId = new Comment("body", "user-id", "article-id");

    assertThat(comment).isNotEqualTo(sameFieldsDifferentId);
    assertThat(comment).isEqualTo(comment);
    assertThat(comment.hashCode()).isEqualTo(comment.hashCode());
  }

  @Test
  public void should_not_be_equal_to_null_or_other_types() {
    Comment comment = new Comment("body", "user-id", "article-id");

    assertThat(comment).isNotEqualTo(null);
    assertThat(comment).isNotEqualTo("not a comment");
  }

  @Test
  public void should_create_empty_comment_with_no_args_constructor() {
    Comment comment = new Comment();

    assertThat(comment.getId()).isNull();
    assertThat(comment.getBody()).isNull();
    assertThat(comment.getCreatedAt()).isNull();
  }
}
