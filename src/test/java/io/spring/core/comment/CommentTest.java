package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_create_comment_with_all_fields() {
    String body = "This is a test comment";
    String userId = "user-123";
    String articleId = "article-456";

    Comment comment = new Comment(body, userId, articleId);

    assertThat(comment.getId(), notNullValue());
    assertThat(comment.getBody(), is(body));
    assertThat(comment.getUserId(), is(userId));
    assertThat(comment.getArticleId(), is(articleId));
    assertThat(comment.getCreatedAt(), notNullValue());
  }

  @Test
  public void should_generate_unique_id_for_each_comment() {
    Comment comment1 = new Comment("Comment 1", "user1", "article1");
    Comment comment2 = new Comment("Comment 2", "user2", "article2");

    assertThat(comment1.getId(), not(comment2.getId()));
  }

  @Test
  public void should_set_created_at_timestamp() {
    Comment comment = new Comment("Test comment", "user-id", "article-id");

    assertThat(comment.getCreatedAt(), notNullValue());
  }

  @Test
  public void should_store_body_correctly() {
    String longBody =
        "This is a very long comment body that contains multiple sentences. "
            + "It should be stored correctly without any truncation or modification. "
            + "The comment system should handle long text without issues.";

    Comment comment = new Comment(longBody, "user-id", "article-id");

    assertThat(comment.getBody(), is(longBody));
  }

  @Test
  public void should_store_user_id_correctly() {
    String userId = "user-with-special-id-12345";

    Comment comment = new Comment("Test body", userId, "article-id");

    assertThat(comment.getUserId(), is(userId));
  }

  @Test
  public void should_store_article_id_correctly() {
    String articleId = "article-with-special-id-67890";

    Comment comment = new Comment("Test body", "user-id", articleId);

    assertThat(comment.getArticleId(), is(articleId));
  }

  @Test
  public void should_have_equal_comments_with_same_id() {
    Comment comment1 = new Comment("Body 1", "user1", "article1");
    Comment comment2 = comment1;

    assertThat(comment1.equals(comment2), is(true));
    assertThat(comment1.hashCode(), is(comment2.hashCode()));
  }

  @Test
  public void should_have_different_comments_with_different_ids() {
    Comment comment1 = new Comment("Body 1", "user1", "article1");
    Comment comment2 = new Comment("Body 2", "user2", "article2");

    assertThat(comment1.equals(comment2), is(false));
  }

  @Test
  public void should_handle_empty_body() {
    Comment comment = new Comment("", "user-id", "article-id");

    assertThat(comment.getBody(), is(""));
  }

  @Test
  public void should_create_multiple_comments_for_same_article() {
    String articleId = "shared-article-id";

    Comment comment1 = new Comment("First comment", "user1", articleId);
    Comment comment2 = new Comment("Second comment", "user2", articleId);

    assertThat(comment1.getArticleId(), is(articleId));
    assertThat(comment2.getArticleId(), is(articleId));
    assertThat(comment1.getId(), not(comment2.getId()));
  }

  @Test
  public void should_create_multiple_comments_by_same_user() {
    String userId = "shared-user-id";

    Comment comment1 = new Comment("First comment", userId, "article1");
    Comment comment2 = new Comment("Second comment", userId, "article2");

    assertThat(comment1.getUserId(), is(userId));
    assertThat(comment2.getUserId(), is(userId));
    assertThat(comment1.getId(), not(comment2.getId()));
  }
}
