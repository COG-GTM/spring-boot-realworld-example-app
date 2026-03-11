package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_create_comment_with_all_fields() {
    String body = "This is a test comment";
    String userId = "user123";
    String articleId = "article456";

    Comment comment = new Comment(body, userId, articleId);

    assertThat(comment.getId(), notNullValue());
    assertThat(comment.getBody(), is(body));
    assertThat(comment.getUserId(), is(userId));
    assertThat(comment.getArticleId(), is(articleId));
    assertThat(comment.getCreatedAt(), notNullValue());
  }

  @Test
  public void should_generate_unique_id_for_each_comment() {
    Comment comment1 = new Comment("body1", "user1", "article1");
    Comment comment2 = new Comment("body2", "user2", "article2");

    assertThat(comment1.getId(), notNullValue());
    assertThat(comment2.getId(), notNullValue());
  }

  @Test
  public void should_have_same_equality_for_same_id() {
    Comment comment = new Comment("body", "user", "article");

    assertThat(comment, is(comment));
    assertThat(comment.hashCode(), is(comment.hashCode()));
  }
}
