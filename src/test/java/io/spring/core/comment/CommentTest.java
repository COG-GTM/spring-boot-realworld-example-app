package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_create_comment_with_all_fields() {
    Comment comment = new Comment("comment body", "user-1", "article-1");
    assertThat(comment.getId(), notNullValue());
    assertThat(comment.getBody(), is("comment body"));
    assertThat(comment.getUserId(), is("user-1"));
    assertThat(comment.getArticleId(), is("article-1"));
    assertThat(comment.getCreatedAt(), notNullValue());
  }

  @Test
  public void should_generate_unique_ids() {
    Comment c1 = new Comment("body1", "user-1", "article-1");
    Comment c2 = new Comment("body2", "user-1", "article-1");
    assertThat(c1.getId(), not(c2.getId()));
  }

  @Test
  public void should_have_equality_based_on_id() {
    Comment c1 = new Comment("body", "user-1", "article-1");
    Comment c2 = new Comment("body", "user-1", "article-1");
    assertThat(c1.equals(c2), is(false));
    assertThat(c1.equals(c1), is(true));
  }

  @Test
  public void should_store_different_user_and_article_ids() {
    Comment comment = new Comment("body", "user-42", "article-99");
    assertThat(comment.getUserId(), is("user-42"));
    assertThat(comment.getArticleId(), is("article-99"));
  }
}
