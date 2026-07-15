package io.spring.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class AuthorizationServiceTest {

  @Test
  void articleOwnerCanWriteArticle() {
    User owner = user("owner");
    Article article = article(owner);

    assertThat(AuthorizationService.canWriteArticle(owner, article)).isTrue();
  }

  @Test
  void nonOwnerCannotWriteArticle() {
    User owner = user("owner");
    User otherUser = user("other");
    Article article = article(owner);

    assertThat(AuthorizationService.canWriteArticle(otherUser, article)).isFalse();
  }

  @Test
  void articleAuthorCanWriteComment() {
    User articleAuthor = user("article-author");
    User commentAuthor = user("comment-author");
    Article article = article(articleAuthor);
    Comment comment = comment(commentAuthor, article);

    assertThat(AuthorizationService.canWriteComment(articleAuthor, article, comment)).isTrue();
  }

  @Test
  void commentAuthorCanWriteComment() {
    User articleAuthor = user("article-author");
    User commentAuthor = user("comment-author");
    Article article = article(articleAuthor);
    Comment comment = comment(commentAuthor, article);

    assertThat(AuthorizationService.canWriteComment(commentAuthor, article, comment)).isTrue();
  }

  @Test
  void unrelatedUserCannotWriteComment() {
    User articleAuthor = user("article-author");
    User commentAuthor = user("comment-author");
    User otherUser = user("other");
    Article article = article(articleAuthor);
    Comment comment = comment(commentAuthor, article);

    assertThat(AuthorizationService.canWriteComment(otherUser, article, comment)).isFalse();
  }

  private User user(String username) {
    return new User(username + "@example.com", username, "password", "", "");
  }

  private Article article(User author) {
    return new Article("Title", "Description", "Body", Collections.emptyList(), author.getId());
  }

  private Comment comment(User author, Article article) {
    return new Comment("Comment", author.getId(), article.getId());
  }
}
