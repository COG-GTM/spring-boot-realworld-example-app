package io.spring.application.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.spring.application.DateTimeCursor;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CommentDataTest {

  @Test
  public void should_create_comment_data_with_all_fields() {
    DateTime now = DateTime.now();
    ProfileData author = new ProfileData("author-id", "author", "bio", "image", false);

    CommentData commentData =
        new CommentData("comment-id", "Comment body", "article-id", now, now, author);

    assertThat(commentData.getId(), is("comment-id"));
    assertThat(commentData.getBody(), is("Comment body"));
    assertThat(commentData.getArticleId(), is("article-id"));
    assertThat(commentData.getCreatedAt(), is(now));
    assertThat(commentData.getUpdatedAt(), is(now));
    assertThat(commentData.getProfileData(), is(author));
  }

  @Test
  public void should_create_empty_comment_data() {
    CommentData commentData = new CommentData();

    assertThat(commentData.getId(), is((String) null));
    assertThat(commentData.getBody(), is((String) null));
    assertThat(commentData.getArticleId(), is((String) null));
    assertThat(commentData.getCreatedAt(), is((DateTime) null));
    assertThat(commentData.getUpdatedAt(), is((DateTime) null));
    assertThat(commentData.getProfileData(), is((ProfileData) null));
  }

  @Test
  public void should_get_cursor_from_created_at() {
    DateTime now = DateTime.now();
    CommentData commentData = new CommentData();
    commentData.setCreatedAt(now);

    DateTimeCursor cursor = commentData.getCursor();

    assertNotNull(cursor);
    assertThat(cursor.getData(), is(now));
  }

  @Test
  public void should_set_comment_data_fields() {
    DateTime now = DateTime.now();
    ProfileData author = new ProfileData("author-id", "author", "bio", "image", false);

    CommentData commentData = new CommentData();
    commentData.setId("new-id");
    commentData.setBody("New body");
    commentData.setArticleId("new-article-id");
    commentData.setCreatedAt(now);
    commentData.setUpdatedAt(now);
    commentData.setProfileData(author);

    assertThat(commentData.getId(), is("new-id"));
    assertThat(commentData.getBody(), is("New body"));
    assertThat(commentData.getArticleId(), is("new-article-id"));
    assertThat(commentData.getCreatedAt(), is(now));
    assertThat(commentData.getUpdatedAt(), is(now));
    assertThat(commentData.getProfileData(), is(author));
  }
}
