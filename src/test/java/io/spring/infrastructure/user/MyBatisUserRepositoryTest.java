package io.spring.infrastructure.user;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleFavoriteRepository;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisCommentRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
  MyBatisUserRepository.class,
  MyBatisArticleRepository.class,
  MyBatisCommentRepository.class,
  MyBatisArticleFavoriteRepository.class
})
public class MyBatisUserRepositoryTest extends DbTestBase {
  @Autowired private UserRepository userRepository;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private CommentRepository commentRepository;
  @Autowired private ArticleFavoriteRepository articleFavoriteRepository;
  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("aisensiy@163.com", "aisensiy", "123", "", "default");
  }

  @Test
  public void should_save_and_fetch_user_success() {
    userRepository.save(user);
    Optional<User> userOptional = userRepository.findByUsername("aisensiy");
    Assertions.assertEquals(userOptional.get(), user);
    Optional<User> userOptional2 = userRepository.findByEmail("aisensiy@163.com");
    Assertions.assertEquals(userOptional2.get(), user);
  }

  @Test
  public void should_update_user_success() {
    String newEmail = "newemail@email.com";
    user.update(newEmail, "", "", "", "");
    userRepository.save(user);
    Optional<User> optional = userRepository.findByUsername(user.getUsername());
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals(optional.get().getEmail(), newEmail);

    String newUsername = "newUsername";
    user.update("", newUsername, "", "", "");
    userRepository.save(user);
    optional = userRepository.findByEmail(user.getEmail());
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals(optional.get().getUsername(), newUsername);
    Assertions.assertEquals(optional.get().getImage(), user.getImage());
  }

  @Test
  public void should_create_new_user_follow_success() {
    User other = new User("other@example.com", "other", "123", "", "");
    userRepository.save(other);

    FollowRelation followRelation = new FollowRelation(user.getId(), other.getId());
    userRepository.saveRelation(followRelation);
    Assertions.assertTrue(userRepository.findRelation(user.getId(), other.getId()).isPresent());
  }

  @Test
  public void should_unfollow_user_success() {
    User other = new User("other@example.com", "other", "123", "", "");
    userRepository.save(other);

    FollowRelation followRelation = new FollowRelation(user.getId(), other.getId());
    userRepository.saveRelation(followRelation);

    userRepository.removeRelation(followRelation);
    Assertions.assertFalse(userRepository.findRelation(user.getId(), other.getId()).isPresent());
  }

  @Test
  public void should_remove_user_and_cascade_related_data() {
    userRepository.save(user);

    Article article =
        new Article("test", "desc", "body", Arrays.asList("java"), user.getId());
    articleRepository.save(article);

    Comment comment = new Comment("nice article", user.getId(), article.getId());
    commentRepository.save(comment);

    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    articleFavoriteRepository.save(favorite);

    User other = new User("other@example.com", "other", "123", "", "");
    userRepository.save(other);
    userRepository.saveRelation(new FollowRelation(user.getId(), other.getId()));

    userRepository.remove(user);

    Assertions.assertFalse(userRepository.findById(user.getId()).isPresent());
    Assertions.assertFalse(articleRepository.findById(article.getId()).isPresent());
    Assertions.assertFalse(userRepository.findRelation(user.getId(), other.getId()).isPresent());
  }
}
