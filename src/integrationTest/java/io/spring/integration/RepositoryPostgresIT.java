package io.spring.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.mybatis.mapper.ArticleMapper;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RepositoryPostgresIT extends PostgresIntegrationTest {

  @Autowired private UserRepository userRepository;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private CommentRepository commentRepository;
  @Autowired private ArticleFavoriteRepository articleFavoriteRepository;
  @Autowired private ArticleMapper articleMapper;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User("jake@jake.jake", "jake", "secret", "bio", "image");
    userRepository.save(user);
  }

  @Test
  void should_save_find_and_update_user() {
    assertThat(userRepository.findByUsername("jake")).map(User::getId).contains(user.getId());
    assertThat(userRepository.findByEmail("jake@jake.jake")).isPresent();

    user.update("new@jake.jake", "newjake", "", "new bio", "");
    userRepository.save(user);

    User fetched = userRepository.findById(user.getId()).get();
    assertThat(fetched.getEmail()).isEqualTo("new@jake.jake");
    assertThat(fetched.getUsername()).isEqualTo("newjake");
    assertThat(fetched.getBio()).isEqualTo("new bio");
    assertThat(fetched.getPassword()).isEqualTo("secret");
  }

  @Test
  void should_save_and_remove_follow_relation() {
    User other = new User("other@example.com", "other", "secret", "", "");
    userRepository.save(other);

    FollowRelation relation = new FollowRelation(user.getId(), other.getId());
    userRepository.saveRelation(relation);
    assertThat(userRepository.findRelation(user.getId(), other.getId())).isPresent();

    userRepository.removeRelation(relation);
    assertThat(userRepository.findRelation(user.getId(), other.getId())).isEmpty();
  }

  @Test
  void should_save_update_and_delete_article_with_tags() {
    Article article =
        new Article(
            "How to train your dragon", "desc", "body", Arrays.asList("java", "pg"), user.getId());
    articleRepository.save(article);

    Article fetched = articleRepository.findBySlug("how-to-train-your-dragon").get();
    assertThat(fetched.getId()).isEqualTo(article.getId());
    assertThat(fetched.getCreatedAt().getMillis()).isEqualTo(article.getCreatedAt().getMillis());
    assertThat(fetched.getTags().stream().map(Tag::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("java", "pg");

    fetched.update("Updated title", "", "new body");
    articleRepository.save(fetched);
    Article updated = articleRepository.findById(article.getId()).get();
    assertThat(updated.getSlug()).isEqualTo("updated-title");
    assertThat(updated.getBody()).isEqualTo("new body");
    assertThat(updated.getDescription()).isEqualTo("desc");

    articleRepository.remove(updated);
    assertThat(articleRepository.findById(article.getId())).isEmpty();
  }

  @Test
  void should_roll_back_new_tags_when_article_insert_violates_unique_slug() {
    articleRepository.save(
        new Article("duplicate", "desc", "body", Arrays.asList("java"), user.getId()));
    Article duplicate =
        new Article("duplicate", "desc", "body", Arrays.asList("java", "rollback"), user.getId());

    assertThatThrownBy(() -> articleRepository.save(duplicate)).isInstanceOf(Exception.class);
    assertThat(articleMapper.findTag("rollback")).isNull();
    assertThat(articleMapper.findTag("java")).isNotNull();
  }

  @Test
  void should_save_find_and_remove_comment() {
    Article article = new Article("commented", "desc", "body", Arrays.asList(), user.getId());
    articleRepository.save(article);
    Comment comment = new Comment("nice", user.getId(), article.getId());
    commentRepository.save(comment);

    Optional<Comment> fetched = commentRepository.findById(article.getId(), comment.getId());
    assertThat(fetched).map(Comment::getBody).contains("nice");

    commentRepository.remove(comment);
    assertThat(commentRepository.findById(article.getId(), comment.getId())).isEmpty();
  }

  @Test
  void should_save_and_remove_favorite() {
    Article article = new Article("favorited", "desc", "body", Arrays.asList(), user.getId());
    articleRepository.save(article);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    articleFavoriteRepository.save(favorite);

    assertThat(articleFavoriteRepository.find(article.getId(), user.getId())).isPresent();

    articleFavoriteRepository.remove(favorite);
    assertThat(articleFavoriteRepository.find(article.getId(), user.getId())).isEmpty();
  }
}
