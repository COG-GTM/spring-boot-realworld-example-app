package io.spring.core.comment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  void should_create_comment_with_all_fields() {
    Comment comment = new Comment("This is a comment", "user-123", "article-456");

    assertEquals("This is a comment", comment.getBody());
    assertEquals("user-123", comment.getUserId());
    assertEquals("article-456", comment.getArticleId());
    assertNotNull(comment.getCreatedAt());
  }

  @Test
  void should_generate_uuid_id() {
    Comment comment = new Comment("body", "user-1", "article-1");

    assertNotNull(comment.getId());
    assertFalse(comment.getId().isEmpty());
  }

  @Test
  void should_generate_unique_ids_for_different_comments() {
    Comment comment1 = new Comment("body1", "user-1", "article-1");
    Comment comment2 = new Comment("body2", "user-1", "article-1");

    assertNotEquals(comment1.getId(), comment2.getId());
  }
}
