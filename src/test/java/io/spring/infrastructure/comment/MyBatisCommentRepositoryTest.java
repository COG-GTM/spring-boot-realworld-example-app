package io.spring.infrastructure.comment;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisCommentRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({MyBatisCommentRepository.class, MyBatisUserRepository.class, MyBatisArticleRepository.class})
public class MyBatisCommentRepositoryTest extends DbTestBase {
  @Autowired private CommentRepository commentRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ArticleRepository articleRepository;

  @Test
  public void should_create_and_fetch_comment_success() {
    User user = new User("test@example.com", "testuser", "123", "", "");
    userRepository.save(user);
    
    Article article = new Article("title", "desc", "body", Arrays.asList("tag"), user.getId());
    articleRepository.save(article);
    
    Comment comment = new Comment("content", user.getId(), article.getId());
    commentRepository.save(comment);

    Optional<Comment> optional = commentRepository.findById(article.getId(), comment.getId());
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals(optional.get(), comment);
  }
}
