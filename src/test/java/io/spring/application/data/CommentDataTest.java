package io.spring.application.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.DateTimeCursor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class CommentDataTest {

  @Test
  public void should_create_comment_data_with_all_args_constructor() {
    DateTime now = new DateTime(DateTimeZone.UTC);
    ProfileData profile = new ProfileData("userId", "username", "bio", "image", false);
    CommentData commentData = new CommentData("id123", "comment body", "articleId", now, now, profile);
    
    assertThat(commentData.getId(), is("id123"));
    assertThat(commentData.getBody(), is("comment body"));
    assertThat(commentData.getArticleId(), is("articleId"));
    assertThat(commentData.getCreatedAt(), is(now));
    assertThat(commentData.getUpdatedAt(), is(now));
    assertThat(commentData.getProfileData(), is(profile));
  }

  @Test
  public void should_create_empty_comment_data_with_no_args_constructor() {
    CommentData commentData = new CommentData();
    
    assertThat(commentData.getId(), nullValue());
    assertThat(commentData.getBody(), nullValue());
    assertThat(commentData.getArticleId(), nullValue());
    assertThat(commentData.getCreatedAt(), nullValue());
    assertThat(commentData.getUpdatedAt(), nullValue());
    assertThat(commentData.getProfileData(), nullValue());
  }

  @Test
  public void should_return_cursor_based_on_created_at() {
    DateTime createdAt = new DateTime(2023, 6, 15, 10, 30, 0, DateTimeZone.UTC);
    DateTime updatedAt = new DateTime(2023, 6, 16, 10, 30, 0, DateTimeZone.UTC);
    ProfileData profile = new ProfileData("userId", "username", "bio", "image", false);
    CommentData commentData = new CommentData("id123", "body", "articleId", createdAt, updatedAt, profile);
    
    DateTimeCursor cursor = commentData.getCursor();
    
    assertThat(cursor, notNullValue());
    assertThat(cursor.getData(), is(createdAt));
  }

  @Test
  public void should_allow_setting_fields() {
    CommentData commentData = new CommentData();
    DateTime now = new DateTime(DateTimeZone.UTC);
    ProfileData profile = new ProfileData("userId", "username", "bio", "image", false);
    
    commentData.setId("id123");
    commentData.setBody("comment body");
    commentData.setArticleId("articleId");
    commentData.setCreatedAt(now);
    commentData.setUpdatedAt(now);
    commentData.setProfileData(profile);
    
    assertThat(commentData.getId(), is("id123"));
    assertThat(commentData.getBody(), is("comment body"));
    assertThat(commentData.getArticleId(), is("articleId"));
    assertThat(commentData.getCreatedAt(), is(now));
    assertThat(commentData.getUpdatedAt(), is(now));
    assertThat(commentData.getProfileData(), is(profile));
  }
}
