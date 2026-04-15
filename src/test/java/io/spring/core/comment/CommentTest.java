package io.spring.core.comment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  void should_create_comment_with_all_fields() {
    Comment comment = new Comment("comment body", "userId1", "articleId1");
    assertNotNull(comment.getId());
    assertEquals("comment body", comment.getBody());
    assertEquals("userId1", comment.getUserId());
    assertEquals("articleId1", comment.getArticleId());
    assertNotNull(comment.getCreatedAt());
  }

  @Test
  void should_generate_unique_ids() {
    Comment comment1 = new Comment("body1", "userId1", "articleId1");
    Comment comment2 = new Comment("body2", "userId2", "articleId2");
    assertNotEquals(comment1.getId(), comment2.getId());
  }

  @Test
  void should_have_equality_based_on_id() {
    Comment comment1 = new Comment("body1", "userId1", "articleId1");
    Comment comment2 = new Comment("body2", "userId2", "articleId2");
    assertNotEquals(comment1, comment2);
    assertEquals(comment1, comment1);
  }
}
