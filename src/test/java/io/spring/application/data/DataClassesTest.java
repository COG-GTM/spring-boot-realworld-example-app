package io.spring.application.data;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class DataClassesTest {

  @Test
  public void should_create_article_data_list() {
    ArticleDataList list = new ArticleDataList(Collections.emptyList(), 0);
    assertNotNull(list.getArticleDatas());
    assertEquals(0, list.getCount());
  }

  @Test
  public void should_create_article_data_list_with_count() {
    ArticleDataList list = new ArticleDataList(Collections.emptyList(), 5);
    assertEquals(5, list.getCount());
  }

  @Test
  public void should_create_article_favorite_count() {
    ArticleFavoriteCount afc = new ArticleFavoriteCount("art1", 10);
    assertEquals("art1", afc.getId());
    assertEquals(10, afc.getCount());
  }

  @Test
  public void should_create_user_with_token() {
    UserData userData = new UserData("id1", "email@test.com", "user1", "bio", "image");
    UserWithToken uwt = new UserWithToken(userData, "jwt-token");
    assertEquals("user1", uwt.getUsername());
    assertEquals("jwt-token", uwt.getToken());
    assertEquals("email@test.com", uwt.getEmail());
    assertEquals("bio", uwt.getBio());
    assertEquals("image", uwt.getImage());
  }

  @Test
  public void should_create_user_data() {
    UserData userData = new UserData("id1", "email@test.com", "user1", "bio", "image");
    assertEquals("id1", userData.getId());
    assertEquals("email@test.com", userData.getEmail());
    assertEquals("user1", userData.getUsername());
    assertEquals("bio", userData.getBio());
    assertEquals("image", userData.getImage());
  }

  @Test
  public void should_create_profile_data() {
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", true);
    assertEquals("id1", profile.getId());
    assertEquals("user1", profile.getUsername());
    assertEquals("bio", profile.getBio());
    assertEquals("image", profile.getImage());
    assertTrue(profile.isFollowing());
  }

  @Test
  public void should_set_following_on_profile() {
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", false);
    assertFalse(profile.isFollowing());
    profile.setFollowing(true);
    assertTrue(profile.isFollowing());
  }

  @Test
  public void should_create_comment_data() {
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", false);
    DateTime now = new DateTime();
    CommentData comment = new CommentData("c1", "body", "art1", now, now, profile);
    assertEquals("c1", comment.getId());
    assertEquals("body", comment.getBody());
    assertEquals("art1", comment.getArticleId());
    assertEquals(now, comment.getCreatedAt());
    assertEquals(now, comment.getUpdatedAt());
    assertNotNull(comment.getProfileData());
  }

  @Test
  public void should_create_article_data() {
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", false);
    DateTime now = new DateTime();
    ArticleData article = new ArticleData(
        "art1", "test-slug", "Title", "desc", "body",
        false, 0, now, now,
        Arrays.asList("java"), profile);
    assertEquals("art1", article.getId());
    assertEquals("test-slug", article.getSlug());
    assertEquals("Title", article.getTitle());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
    assertFalse(article.isFavorited());
    assertEquals(0, article.getFavoritesCount());
    assertEquals(1, article.getTagList().size());
  }

  @Test
  public void should_set_favorited_on_article_data() {
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", false);
    DateTime now = new DateTime();
    ArticleData article = new ArticleData(
        "art1", "slug", "Title", "desc", "body",
        false, 0, now, now,
        Collections.emptyList(), profile);
    article.setFavorited(true);
    assertTrue(article.isFavorited());
  }

  @Test
  public void should_set_favorites_count_on_article_data() {
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", false);
    DateTime now = new DateTime();
    ArticleData article = new ArticleData(
        "art1", "slug", "Title", "desc", "body",
        false, 0, now, now,
        Collections.emptyList(), profile);
    article.setFavoritesCount(42);
    assertEquals(42, article.getFavoritesCount());
  }

  @Test
  public void should_get_cursor_from_article_data() {
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", false);
    DateTime now = new DateTime();
    ArticleData article = new ArticleData(
        "art1", "slug", "Title", "desc", "body",
        false, 0, now, now,
        Collections.emptyList(), profile);
    assertNotNull(article.getCursor());
  }

  @Test
  public void should_get_cursor_from_comment_data() {
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", false);
    DateTime now = new DateTime();
    CommentData comment = new CommentData("c1", "body", "art1", now, now, profile);
    assertNotNull(comment.getCursor());
  }

  @Test
  public void should_get_email_from_user_with_token() {
    UserData userData = new UserData("id1", "email@test.com", "user1", "bio", "image");
    UserWithToken uwt = new UserWithToken(userData, "token");
    assertEquals("user1", uwt.getUsername());
  }
}
