package io.spring.infrastructure.comment;

import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisCommentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({MyBatisCommentRepository.class})
public class MyBatisCommentRepositoryTest extends DbTestBase {
  @Autowired private CommentRepository commentRepository;

  @Test
  public void should_create_and_fetch_comment_success() {
    Comment comment = new Comment("content", "123", "456");
    commentRepository.save(comment);

    Optional<Comment> optional = commentRepository.findById("456", comment.getId());
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals(optional.get(), comment);
    Assertions.assertEquals(optional.get().getBody(), "content");
    Assertions.assertEquals(optional.get().getUserId(), "123");
    Assertions.assertEquals(optional.get().getArticleId(), "456");
    Assertions.assertNotNull(optional.get().getCreatedAt());
  }

  @Test
  public void should_get_empty_optional_for_unknown_comment_id() {
    Comment comment = new Comment("content", "123", "456");
    commentRepository.save(comment);

    Assertions.assertFalse(commentRepository.findById("456", "not-exists-id").isPresent());
  }

  @Test
  public void should_get_empty_optional_when_comment_belongs_to_another_article() {
    Comment comment = new Comment("content", "123", "456");
    commentRepository.save(comment);

    Assertions.assertFalse(commentRepository.findById("789", comment.getId()).isPresent());
  }

  @Test
  public void should_remove_comment_success() {
    Comment comment = new Comment("content", "123", "456");
    commentRepository.save(comment);

    commentRepository.remove(comment);

    Assertions.assertFalse(commentRepository.findById("456", comment.getId()).isPresent());
  }

  @Test
  public void should_only_remove_the_target_comment() {
    Comment comment = new Comment("content", "123", "456");
    Comment otherComment = new Comment("other content", "123", "456");
    commentRepository.save(comment);
    commentRepository.save(otherComment);

    commentRepository.remove(comment);

    Assertions.assertFalse(commentRepository.findById("456", comment.getId()).isPresent());
    Assertions.assertTrue(commentRepository.findById("456", otherComment.getId()).isPresent());
  }
}
