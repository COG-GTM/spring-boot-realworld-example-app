package io.spring.core.comment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_create_comment_with_all_fields() {
    Comment comment = new Comment("This is a comment body", "user-123", "article-456");

    assertNotNull(comment.getId());
    assertEquals("This is a comment body", comment.getBody());
    assertEquals("user-123", comment.getUserId());
    assertEquals("article-456", comment.getArticleId());
    assertNotNull(comment.getCreatedAt());
  }

  @Test
  public void should_generate_unique_id_for_each_comment() {
    Comment comment1 = new Comment("body1", "user1", "article1");
    Comment comment2 = new Comment("body2", "user2", "article2");

    assertNotEquals(comment1.getId(), comment2.getId());
  }

  @Test
  public void should_have_equals_based_on_id() {
    Comment comment1 = new Comment("body", "user", "article");
    Comment comment2 = new Comment("body", "user", "article");

    assertNotEquals(comment1, comment2);
    assertEquals(comment1, comment1);
  }

  @Test
  public void should_set_created_at_on_construction() {
    long beforeCreation = System.currentTimeMillis();
    Comment comment = new Comment("body", "user", "article");
    long afterCreation = System.currentTimeMillis();

    long createdAtMillis = comment.getCreatedAt().getMillis();
    assertTrue(createdAtMillis >= beforeCreation && createdAtMillis <= afterCreation);
  }
}
