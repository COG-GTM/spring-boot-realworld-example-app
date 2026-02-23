package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_create_comment_with_all_fields() {
    Comment comment = new Comment("comment body", "user-id-1", "article-id-1");

    assertThat(comment.getId(), notNullValue());
    assertThat(comment.getBody(), is("comment body"));
    assertThat(comment.getUserId(), is("user-id-1"));
    assertThat(comment.getArticleId(), is("article-id-1"));
    assertThat(comment.getCreatedAt(), notNullValue());
  }

  @Test
  public void should_generate_unique_ids_for_different_comments() {
    Comment comment1 = new Comment("body1", "user1", "article1");
    Comment comment2 = new Comment("body2", "user2", "article2");

    assertThat(comment1.getId(), not(comment2.getId()));
  }

  @Test
  public void should_have_equality_based_on_id() {
    Comment comment1 = new Comment("body", "user1", "article1");
    Comment comment2 = new Comment("body", "user1", "article1");

    assertThat(comment1.equals(comment2), is(false));
    assertThat(comment1.equals(comment1), is(true));
  }
}
