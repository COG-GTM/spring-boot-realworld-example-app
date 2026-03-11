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
    Comment comment1 = new Comment("Comment 1", "user123", "article456");
    Comment comment2 = new Comment("Comment 2", "user123", "article456");

    assertThat(comment1.getId(), not(comment2.getId()));
  }

  @Test
  public void should_set_created_at_timestamp_on_creation() {
    long beforeCreation = System.currentTimeMillis();
    Comment comment = new Comment("Test comment", "user123", "article456");
    long afterCreation = System.currentTimeMillis();

    assertThat(comment.getCreatedAt(), notNullValue());
    assertThat(comment.getCreatedAt().getMillis() >= beforeCreation, is(true));
    assertThat(comment.getCreatedAt().getMillis() <= afterCreation, is(true));
  }

  @Test
  public void should_create_comment_with_empty_body() {
    Comment comment = new Comment("", "user123", "article456");

    assertThat(comment.getId(), notNullValue());
    assertThat(comment.getBody(), is(""));
    assertThat(comment.getUserId(), is("user123"));
    assertThat(comment.getArticleId(), is("article456"));
  }

  @Test
  public void should_create_comment_with_long_body() {
    String longBody = "A".repeat(10000);
    Comment comment = new Comment(longBody, "user123", "article456");

    assertThat(comment.getBody(), is(longBody));
    assertThat(comment.getBody().length(), is(10000));
  }

  @Test
  public void should_have_equality_based_on_id() {
    Comment comment1 = new Comment("Same body", "user123", "article456");
    Comment comment2 = new Comment("Same body", "user123", "article456");

    assertThat(comment1.equals(comment2), is(false));
    assertThat(comment1.equals(comment1), is(true));
  }

  @Test
  public void should_have_consistent_hashcode_based_on_id() {
    Comment comment = new Comment("Test comment", "user123", "article456");

    int hashCode1 = comment.hashCode();
    int hashCode2 = comment.hashCode();

    assertThat(hashCode1, is(hashCode2));
  }

  @Test
  public void should_create_comment_with_special_characters_in_body() {
    String specialBody = "Comment with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
    Comment comment = new Comment(specialBody, "user123", "article456");

    assertThat(comment.getBody(), is(specialBody));
  }

  @Test
  public void should_create_comment_with_unicode_characters() {
    String unicodeBody = "Comment with unicode: \u4e2d\u6587 \u65e5\u672c\u8a9e \ud55c\uad6d\uc5b4";
    Comment comment = new Comment(unicodeBody, "user123", "article456");

    assertThat(comment.getBody(), is(unicodeBody));
  }

  @Test
  public void should_create_comment_with_multiline_body() {
    String multilineBody = "Line 1\nLine 2\nLine 3";
    Comment comment = new Comment(multilineBody, "user123", "article456");

    assertThat(comment.getBody(), is(multilineBody));
  }
}
