package io.spring.core.comment;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_generate_id_on_construction() {
    Comment comment = new Comment("body", "userId", "articleId");
    assertThat(comment.getId()).isNotNull();
    assertThat(comment.getId()).isNotEmpty();
  }

  @Test
  public void should_set_all_fields_on_construction() {
    Comment comment = new Comment("body", "userId", "articleId");
    assertThat(comment.getBody()).isEqualTo("body");
    assertThat(comment.getUserId()).isEqualTo("userId");
    assertThat(comment.getArticleId()).isEqualTo("articleId");
  }

  @Test
  public void should_set_created_at_on_construction() {
    DateTime before = new DateTime();
    Comment comment = new Comment("body", "userId", "articleId");
    DateTime after = new DateTime();
    assertThat(comment.getCreatedAt()).isNotNull();
    assertThat(comment.getCreatedAt().isBefore(before.minusSeconds(1))).isFalse();
    assertThat(comment.getCreatedAt().isAfter(after.plusSeconds(1))).isFalse();
  }

  @Test
  public void should_generate_distinct_ids_for_different_comments() {
    Comment first = new Comment("body", "userId", "articleId");
    Comment second = new Comment("body", "userId", "articleId");
    assertThat(first.getId()).isNotEqualTo(second.getId());
  }

  @Test
  public void should_be_equal_when_ids_match() {
    Comment comment = new Comment("body", "userId", "articleId");
    Comment same = new Comment("other", "otherUser", "otherArticle");
    setId(same, comment.getId());
    assertThat(comment).isEqualTo(same);
    assertThat(comment.hashCode()).isEqualTo(same.hashCode());
  }

  @Test
  public void should_not_be_equal_when_ids_differ() {
    Comment first = new Comment("body", "userId", "articleId");
    Comment second = new Comment("body", "userId", "articleId");
    assertThat(first).isNotEqualTo(second);
  }

  private void setId(Comment comment, String id) {
    try {
      java.lang.reflect.Field field = Comment.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(comment, id);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
