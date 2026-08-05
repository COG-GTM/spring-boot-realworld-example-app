package io.spring.core.comment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class CommentTest {

  @Test
  public void should_generate_id_and_created_at_and_keep_all_fields_from_constructor() {
    DateTime before = new DateTime();
    Comment comment = new Comment("content", "userId", "articleId");

    assertThat(comment.getId()).isNotNull();
    assertThat(UUID.fromString(comment.getId()).toString()).isEqualTo(comment.getId());
    assertThat(comment.getBody()).isEqualTo("content");
    assertThat(comment.getUserId()).isEqualTo("userId");
    assertThat(comment.getArticleId()).isEqualTo("articleId");
    assertThat(comment.getCreatedAt()).isNotNull();
    assertThat(comment.getCreatedAt().isBefore(before)).isFalse();
  }

  @Test
  public void should_generate_different_id_for_each_comment() {
    Comment one = new Comment("content", "userId", "articleId");
    Comment another = new Comment("content", "userId", "articleId");

    assertThat(one.getId()).isNotEqualTo(another.getId());
  }

  @Test
  public void should_leave_all_fields_null_with_no_args_constructor() {
    Comment comment = new Comment();

    assertThat(comment.getId()).isNull();
    assertThat(comment.getBody()).isNull();
    assertThat(comment.getUserId()).isNull();
    assertThat(comment.getArticleId()).isNull();
    assertThat(comment.getCreatedAt()).isNull();
  }

  @Test
  public void should_use_only_id_for_equals_and_hashcode() {
    Comment comment = new Comment("content", "userId", "articleId");
    Comment sameIdDifferentContent = new Comment("other content", "otherUser", "otherArticle");
    ReflectionTestUtils.setField(sameIdDifferentContent, "id", comment.getId());

    assertThat(sameIdDifferentContent).isEqualTo(comment);
    assertThat(sameIdDifferentContent.hashCode()).isEqualTo(comment.hashCode());
  }

  @Test
  public void should_not_be_equal_when_ids_are_different() {
    Comment one = new Comment("content", "userId", "articleId");
    Comment another = new Comment("content", "userId", "articleId");

    assertThat(one).isNotEqualTo(another);
  }

  @Test
  public void should_not_be_equal_to_null_or_other_types() {
    Comment comment = new Comment("content", "userId", "articleId");

    assertThat(comment).isNotEqualTo(null);
    assertThat(comment).isNotEqualTo("content");
    assertThat(comment).isEqualTo(comment);
  }

  @Test
  public void should_be_equal_when_both_ids_are_null() {
    assertThat(new Comment()).isEqualTo(new Comment());
    assertThat(new Comment().hashCode()).isEqualTo(new Comment().hashCode());
  }
}
