package io.spring.core.service;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {

  @Test
  public void should_allow_author_to_write_article() {
    User user = new User("a@b.com", "user1", "pass", "", "");
    Article article = new Article("title", "desc", "body", Collections.emptyList(), user.getId());
    assertTrue(AuthorizationService.canWriteArticle(user, article));
  }

  @Test
  public void should_deny_non_author_to_write_article() {
    User author = new User("a@b.com", "author", "pass", "", "");
    User other = new User("c@d.com", "other", "pass", "", "");
    Article article = new Article("title", "desc", "body", Collections.emptyList(), author.getId());
    assertFalse(AuthorizationService.canWriteArticle(other, article));
  }

  @Test
  public void should_allow_article_author_to_write_comment() {
    User articleAuthor = new User("a@b.com", "author", "pass", "", "");
    User commentAuthor = new User("c@d.com", "commenter", "pass", "", "");
    Article article = new Article("title", "desc", "body", Collections.emptyList(), articleAuthor.getId());
    Comment comment = new Comment("comment body", commentAuthor.getId(), article.getId());
    assertTrue(AuthorizationService.canWriteComment(articleAuthor, article, comment));
  }

  @Test
  public void should_allow_comment_author_to_write_comment() {
    User articleAuthor = new User("a@b.com", "author", "pass", "", "");
    User commentAuthor = new User("c@d.com", "commenter", "pass", "", "");
    Article article = new Article("title", "desc", "body", Collections.emptyList(), articleAuthor.getId());
    Comment comment = new Comment("comment body", commentAuthor.getId(), article.getId());
    assertTrue(AuthorizationService.canWriteComment(commentAuthor, article, comment));
  }

  @Test
  public void should_deny_other_user_to_write_comment() {
    User articleAuthor = new User("a@b.com", "author", "pass", "", "");
    User commentAuthor = new User("c@d.com", "commenter", "pass", "", "");
    User otherUser = new User("e@f.com", "other", "pass", "", "");
    Article article = new Article("title", "desc", "body", Collections.emptyList(), articleAuthor.getId());
    Comment comment = new Comment("comment body", commentAuthor.getId(), article.getId());
    assertFalse(AuthorizationService.canWriteComment(otherUser, article, comment));
  }
}
