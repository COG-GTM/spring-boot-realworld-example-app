package io.spring.core.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_assign_id_fields_and_created_at() {
    Comment comment = new Comment("body", "user-id", "article-id");

    assertNotNull(comment.getId());
    assertEquals("body", comment.getBody());
    assertEquals("user-id", comment.getUserId());
    assertEquals("article-id", comment.getArticleId());
    assertNotNull(comment.getCreatedAt());
  }

  @Test
  public void should_use_id_for_equality() {
    Comment first = new Comment("body", "user-id", "article-id");
    Comment second = new Comment("body", "user-id", "article-id");

    assertNotEquals(first, second);
    assertEquals(first, first);
  }
}
