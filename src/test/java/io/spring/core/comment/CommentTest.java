package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_create_comment_with_id_and_created_time() {
    Comment comment = new Comment("body", "userId", "articleId");
    assertThat(comment.getId(), notNullValue());
    assertThat(comment.getCreatedAt(), notNullValue());
    assertThat(comment.getBody(), is("body"));
    assertThat(comment.getUserId(), is("userId"));
    assertThat(comment.getArticleId(), is("articleId"));
  }

  @Test
  public void should_only_be_equal_to_itself() {
    Comment comment = new Comment("body", "userId", "articleId");
    assertThat(comment, is(comment));
    assertThat(comment, not(new Comment("body", "userId", "articleId")));
  }
}
