package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_create_comment_with_id() {
    Comment comment = new Comment("body", "userId", "articleId");
    assertThat(comment.getId(), notNullValue());
  }

  @Test
  public void should_create_comment_with_body() {
    Comment comment = new Comment("test body", "userId", "articleId");
    assertThat(comment.getBody(), is("test body"));
  }

  @Test
  public void should_create_comment_with_user_id() {
    Comment comment = new Comment("body", "user123", "articleId");
    assertThat(comment.getUserId(), is("user123"));
  }

  @Test
  public void should_create_comment_with_article_id() {
    Comment comment = new Comment("body", "userId", "article123");
    assertThat(comment.getArticleId(), is("article123"));
  }

  @Test
  public void should_create_comment_with_created_at_timestamp() {
    Comment comment = new Comment("body", "userId", "articleId");
    assertThat(comment.getCreatedAt(), notNullValue());
  }
}
