package io.spring.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.JacksonCustomizations;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleDataList;
import io.spring.application.data.ArticleFavoriteCount;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.application.data.UserData;
import io.spring.application.data.UserWithToken;
import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

class DataDtoTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper().registerModule(new JacksonCustomizations.RealWorldModules());

  private final DateTime createdAt = new DateTime(2020, 3, 1, 12, 0, 0, 0, DateTimeZone.UTC);
  private final DateTime updatedAt = new DateTime(2020, 3, 2, 12, 0, 0, 0, DateTimeZone.UTC);

  private ProfileData profileData() {
    return new ProfileData("user-id", "jake", "bio", "image", true);
  }

  @Test
  void should_copy_user_data_fields_into_user_with_token() {
    UserData userData = new UserData("id", "jake@jake.jake", "jake", "bio", "image");

    UserWithToken userWithToken = new UserWithToken(userData, "token");

    assertThat(userWithToken.getEmail()).isEqualTo("jake@jake.jake");
    assertThat(userWithToken.getUsername()).isEqualTo("jake");
    assertThat(userWithToken.getBio()).isEqualTo("bio");
    assertThat(userWithToken.getImage()).isEqualTo("image");
    assertThat(userWithToken.getToken()).isEqualTo("token");
  }

  @Test
  void should_serialize_user_with_token_without_id() throws Exception {
    UserWithToken userWithToken =
        new UserWithToken(new UserData("id", "jake@jake.jake", "jake", "bio", "image"), "token");

    String json = objectMapper.writeValueAsString(userWithToken);

    assertThat(objectMapper.readTree(json).get("email").asText()).isEqualTo("jake@jake.jake");
    assertThat(objectMapper.readTree(json).get("token").asText()).isEqualTo("token");
    assertThat(objectMapper.readTree(json).has("id")).isFalse();
  }

  @Test
  void should_respect_equality_and_no_args_constructor_of_user_data() {
    UserData userData = new UserData("id", "jake@jake.jake", "jake", "bio", "image");
    UserData same = new UserData("id", "jake@jake.jake", "jake", "bio", "image");
    UserData other = new UserData("id", "jake@jake.jake", "other", "bio", "image");

    assertThat(userData).isEqualTo(same).hasSameHashCodeAs(same).isNotEqualTo(other);
    assertThat(userData.toString()).contains("jake@jake.jake");

    UserData empty = new UserData();
    empty.setId("id");
    empty.setEmail("jake@jake.jake");
    empty.setUsername("jake");
    empty.setBio("bio");
    empty.setImage("image");
    assertThat(empty).isEqualTo(userData);
  }

  @Test
  void should_hide_id_of_profile_data_when_serializing() throws Exception {
    String json = objectMapper.writeValueAsString(profileData());

    assertThat(objectMapper.readTree(json).has("id")).isFalse();
    assertThat(objectMapper.readTree(json).get("username").asText()).isEqualTo("jake");
    assertThat(objectMapper.readTree(json).get("following").asBoolean()).isTrue();
  }

  @Test
  void should_respect_equality_of_profile_data() {
    ProfileData other = new ProfileData("user-id", "jake", "bio", "image", false);

    assertThat(profileData()).isEqualTo(new ProfileData("user-id", "jake", "bio", "image", true));
    assertThat(profileData()).isNotEqualTo(other);
    assertThat(profileData().hashCode()).isNotEqualTo(other.hashCode());
    assertThat(new ProfileData().getUsername()).isNull();
  }

  @Test
  void should_use_updated_at_as_cursor_of_article_data() {
    ArticleData articleData =
        new ArticleData(
            "id",
            "slug",
            "title",
            "description",
            "body",
            false,
            0,
            createdAt,
            updatedAt,
            Arrays.asList("java", "spring"),
            profileData());

    assertThat(articleData.getCursor().getData()).isEqualTo(updatedAt);
    assertThat(articleData.getTagList()).containsExactly("java", "spring");
    assertThat(articleData.getProfileData()).isEqualTo(profileData());
    assertThat(articleData)
        .isEqualTo(
            new ArticleData(
                "id",
                "slug",
                "title",
                "description",
                "body",
                false,
                0,
                createdAt,
                updatedAt,
                Arrays.asList("java", "spring"),
                profileData()));
    assertThat(new ArticleData().getSlug()).isNull();
  }

  @Test
  void should_serialize_article_data_with_author_field() throws Exception {
    ArticleData articleData =
        new ArticleData(
            "id",
            "slug",
            "title",
            "description",
            "body",
            true,
            3,
            createdAt,
            updatedAt,
            Collections.singletonList("java"),
            profileData());

    String json = objectMapper.writeValueAsString(articleData);

    assertThat(objectMapper.readTree(json).get("author").get("username").asText())
        .isEqualTo("jake");
    assertThat(objectMapper.readTree(json).get("createdAt").asText())
        .isEqualTo("2020-03-01T12:00:00.000Z");
    assertThat(objectMapper.readTree(json).get("favoritesCount").asInt()).isEqualTo(3);
  }

  @Test
  void should_expose_articles_and_count_of_article_data_list() throws Exception {
    ArticleData articleData =
        new ArticleData(
            "id",
            "slug",
            "title",
            "description",
            "body",
            false,
            0,
            createdAt,
            updatedAt,
            Collections.emptyList(),
            profileData());

    ArticleDataList articleDataList =
        new ArticleDataList(Collections.singletonList(articleData), 1);

    assertThat(articleDataList.getArticleDatas()).containsExactly(articleData);
    assertThat(articleDataList.getCount()).isEqualTo(1);

    String json = objectMapper.writeValueAsString(articleDataList);
    assertThat(objectMapper.readTree(json).get("articles")).hasSize(1);
    assertThat(objectMapper.readTree(json).get("articlesCount").asInt()).isEqualTo(1);
  }

  @Test
  void should_use_created_at_as_cursor_of_comment_data_and_hide_article_id() throws Exception {
    CommentData commentData =
        new CommentData("id", "body", "article-id", createdAt, updatedAt, profileData());

    assertThat(commentData.getCursor().getData()).isEqualTo(createdAt);
    assertThat(commentData)
        .isEqualTo(new CommentData("id", "body", "article-id", createdAt, updatedAt, profileData()))
        .isNotEqualTo(
            new CommentData("id", "other", "article-id", createdAt, updatedAt, profileData()));
    assertThat(new CommentData().getBody()).isNull();

    String json = objectMapper.writeValueAsString(commentData);
    assertThat(objectMapper.readTree(json).has("articleId")).isFalse();
    assertThat(objectMapper.readTree(json).get("author").get("username").asText())
        .isEqualTo("jake");
  }

  @Test
  void should_expose_id_and_count_of_article_favorite_count() {
    ArticleFavoriteCount favoriteCount = new ArticleFavoriteCount("article-id", 5);

    assertThat(favoriteCount.getId()).isEqualTo("article-id");
    assertThat(favoriteCount.getCount()).isEqualTo(5);
    assertThat(favoriteCount)
        .isEqualTo(new ArticleFavoriteCount("article-id", 5))
        .isNotEqualTo(new ArticleFavoriteCount("article-id", 6));
    assertThat(favoriteCount.toString()).contains("article-id");
  }
}
