package io.spring.core.comment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  void should_generate_non_null_uuid() {
    Comment comment = new Comment("body", "userId", "articleId");
    assertNotNull(comment.getId());
    assertFalse(comment.getId().isEmpty());
  }

  @Test
  void should_set_fields_correctly() {
    Comment comment = new Comment("body text", "user123", "article456");
    assertEquals("body text", comment.getBody());
    assertEquals("user123", comment.getUserId());
    assertEquals("article456", comment.getArticleId());
  }

  @Test
  void should_set_createdAt_on_creation() {
    Comment comment = new Comment("body", "userId", "articleId");
    assertNotNull(comment.getCreatedAt());
  }

  @Test
  void should_be_equal_when_same_id() {
    Comment c1 = new Comment("body1", "user1", "article1");
    Comment c2 = new Comment("body2", "user2", "article2");
    assertNotEquals(c1, c2);

    try {
      java.lang.reflect.Field idField = Comment.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(c2, c1.getId());
    } catch (Exception e) {
      fail("Reflection failed");
    }
    assertEquals(c1, c2);
    assertEquals(c1.hashCode(), c2.hashCode());
  }
}
