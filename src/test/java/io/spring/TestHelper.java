package io.spring;

import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.application.data.UserData;
import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.ArrayList;
import java.util.Arrays;
import org.joda.time.DateTime;

public class TestHelper {
  public static ArticleData articleDataFixture(String seed, User user) {
    DateTime now = new DateTime();
    return new ArticleData(
        seed + "id",
        "title-" + seed,
        "title " + seed,
        "desc " + seed,
        "body " + seed,
        false,
        0,
        now,
        now,
        new ArrayList<>(),
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
  }

  public static ArticleData getArticleDataFromArticleAndUser(Article article, User user) {
    return new ArticleData(
        article.getId(),
        article.getSlug(),
        article.getTitle(),
        article.getDescription(),
        article.getBody(),
        false,
        0,
        article.getCreatedAt(),
        article.getUpdatedAt(),
        Arrays.asList("joda"),
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
  }

  public static User userFixture(String seed) {
    return new User(
        seed + "@test.com", seed, "123", seed + " bio", "https://images.com/" + seed + ".jpg");
  }

  public static Article articleFixture(String seed, User user) {
    return new Article(
        "title " + seed,
        "desc " + seed,
        "body " + seed,
        Arrays.asList("java", "spring"),
        user.getId());
  }

  public static Comment commentFixture(String seed, Article article, User user) {
    return new Comment("comment " + seed, user.getId(), article.getId());
  }

  public static ProfileData profileDataFixture(User user) {
    return new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
  }

  public static UserData userDataFixture(User user) {
    return new UserData(
        user.getId(), user.getEmail(), user.getUsername(), user.getBio(), user.getImage());
  }

  public static CommentData commentDataFixture(String seed, User user) {
    DateTime now = new DateTime();
    return new CommentData(
        seed + "id", "comment " + seed, seed + "-article-id", now, now, profileDataFixture(user));
  }
}
