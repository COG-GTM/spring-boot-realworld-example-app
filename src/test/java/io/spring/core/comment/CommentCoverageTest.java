package io.spring.core.comment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class CommentCoverageTest {

  @Test
  public void should_create_comment() {
    Comment comment = new Comment("body", "userId", "articleId");

    assertNotNull(comment.getId());
    assertEquals("body", comment.getBody());
    assertEquals("userId", comment.getUserId());
    assertEquals("articleId", comment.getArticleId());
    assertNotNull(comment.getCreatedAt());
  }

  @Test
  public void should_have_no_arg_constructor() {
    Comment comment = new Comment();
    assertNull(comment.getId());
    assertNull(comment.getBody());
  }

  @Test
  public void should_have_equals_based_on_id() {
    Comment comment1 = new Comment("body1", "user1", "article1");
    Comment comment2 = new Comment("body2", "user2", "article2");

    assertNotEquals(comment1, comment2);
    assertEquals(comment1, comment1);
  }

  @Test
  public void should_have_hashcode_based_on_id() {
    Comment comment1 = new Comment("body", "user", "article");
    Comment comment2 = new Comment("body", "user", "article");

    // Different IDs so different hashcodes
    assertNotEquals(comment1.hashCode(), comment2.hashCode());
  }
}
