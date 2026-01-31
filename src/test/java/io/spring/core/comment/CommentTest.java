package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_create_comment_with_all_fields() {
    Comment comment = new Comment("This is a comment body", "user123", "article456");
    
    assertThat(comment.getId(), notNullValue());
    assertThat(comment.getBody(), is("This is a comment body"));
    assertThat(comment.getUserId(), is("user123"));
    assertThat(comment.getArticleId(), is("article456"));
    assertThat(comment.getCreatedAt(), notNullValue());
  }

  @Test
  public void should_generate_unique_id_for_each_comment() {
    Comment comment1 = new Comment("body1", "user1", "article1");
    Comment comment2 = new Comment("body2", "user2", "article2");
    
    assertThat(comment1.getId(), not(comment2.getId()));
  }

  @Test
  public void should_have_equality_based_on_id() {
    Comment comment1 = new Comment("same body", "user1", "article1");
    Comment comment2 = new Comment("same body", "user1", "article1");
    
    assertThat(comment1.equals(comment2), is(false));
    assertThat(comment1.equals(comment1), is(true));
  }

  @Test
  public void should_set_created_at_on_construction() {
    long beforeCreation = System.currentTimeMillis();
    Comment comment = new Comment("body", "user", "article");
    long afterCreation = System.currentTimeMillis();
    
    assertThat(comment.getCreatedAt().getMillis() >= beforeCreation, is(true));
    assertThat(comment.getCreatedAt().getMillis() <= afterCreation, is(true));
  }
}
