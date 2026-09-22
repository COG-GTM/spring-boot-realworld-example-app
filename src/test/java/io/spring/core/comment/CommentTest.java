package io.spring.core.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_set_all_fields_on_creation() {
    Comment comment = new Comment("content", "userId", "articleId");

    assertNotNull(comment.getId());
    assertNotNull(comment.getCreatedAt());
    assertEquals("content", comment.getBody());
    assertEquals("userId", comment.getUserId());
    assertEquals("articleId", comment.getArticleId());
  }

  @Test
  public void should_compare_comments_by_id() {
    Comment comment = new Comment("content", "userId", "articleId");
    Comment another = new Comment("content", "userId", "articleId");

    assertEquals(comment, comment);
    assertNotEquals(comment, another);
  }
}
