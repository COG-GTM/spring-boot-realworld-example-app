package io.spring.infrastructure.article;

import io.spring.application.Page;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.mybatis.readservice.ArticleReadService;
import io.spring.infrastructure.repository.MyBatisArticleFavoriteRepository;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
  MyBatisUserRepository.class,
  MyBatisArticleRepository.class,
  MyBatisArticleFavoriteRepository.class
})
public class ArticleReadServiceCountTest extends DbTestBase {
  @Autowired private ArticleReadService articleReadService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private ArticleFavoriteRepository articleFavoriteRepository;

  @Autowired private DataSource dataSource;

  private User author;
  private User otherAuthor;
  private User fan1;
  private User fan2;
  private Article multiTagArticle;
  private Article otherAuthorArticle;
  private Article untaggedArticle;

  @BeforeEach
  public void setUp() {
    author = saveUser("author");
    otherAuthor = saveUser("other");
    fan1 = saveUser("fan1");
    fan2 = saveUser("fan2");

    multiTagArticle =
        new Article(
            "multi tag",
            "desc",
            "body",
            Arrays.asList("java", "spring", "sql"),
            author.getId(),
            new DateTime());
    otherAuthorArticle =
        new Article(
            "other article",
            "desc",
            "body",
            Arrays.asList("java"),
            otherAuthor.getId(),
            new DateTime().minusHours(1));
    untaggedArticle =
        new Article(
            "untagged",
            "desc",
            "body",
            Arrays.asList(),
            author.getId(),
            new DateTime().minusHours(2));
    articleRepository.save(multiTagArticle);
    articleRepository.save(otherAuthorArticle);
    articleRepository.save(untaggedArticle);

    articleFavoriteRepository.save(new ArticleFavorite(multiTagArticle.getId(), fan1.getId()));
    articleFavoriteRepository.save(new ArticleFavorite(multiTagArticle.getId(), fan2.getId()));
    articleFavoriteRepository.save(new ArticleFavorite(otherAuthorArticle.getId(), fan1.getId()));
  }

  private User saveUser(String username) {
    User user = new User(username + "@test.com", username, "123", "", "");
    userRepository.save(user);
    return user;
  }

  @Test
  public void should_count_all_articles_once_without_filters() {
    Assertions.assertEquals(3, articleReadService.countArticle(null, null, null));
  }

  @Test
  public void should_count_tagged_article_once_despite_multiple_favorites() {
    Assertions.assertEquals(2, articleReadService.countArticle("java", null, null));
    Assertions.assertEquals(1, articleReadService.countArticle("spring", null, null));
    Assertions.assertEquals(0, articleReadService.countArticle("missing", null, null));
  }

  @Test
  public void should_count_by_author_including_untagged_articles() {
    Assertions.assertEquals(2, articleReadService.countArticle(null, author.getUsername(), null));
    Assertions.assertEquals(
        1, articleReadService.countArticle(null, otherAuthor.getUsername(), null));
    Assertions.assertEquals(0, articleReadService.countArticle(null, "nobody", null));
  }

  @Test
  public void should_count_favorited_article_once_despite_multiple_tags() {
    Assertions.assertEquals(2, articleReadService.countArticle(null, null, fan1.getUsername()));
    Assertions.assertEquals(1, articleReadService.countArticle(null, null, fan2.getUsername()));
    Assertions.assertEquals(0, articleReadService.countArticle(null, null, author.getUsername()));
    Assertions.assertEquals(0, articleReadService.countArticle(null, null, "nobody"));
  }

  @Test
  public void should_apply_all_filters_together() {
    Assertions.assertEquals(
        1, articleReadService.countArticle("java", author.getUsername(), fan2.getUsername()));
    Assertions.assertEquals(
        1, articleReadService.countArticle("java", otherAuthor.getUsername(), fan1.getUsername()));
    Assertions.assertEquals(
        0, articleReadService.countArticle("spring", otherAuthor.getUsername(), null));
    Assertions.assertEquals(
        0, articleReadService.countArticle(null, otherAuthor.getUsername(), fan2.getUsername()));
    Assertions.assertEquals(1, articleReadService.countArticle("sql", null, fan1.getUsername()));
  }

  @Test
  public void should_match_number_of_ids_returned_by_query_articles() {
    String[][] filters = {
      {null, null, null},
      {"java", null, null},
      {"sql", null, null},
      {null, author.getUsername(), null},
      {null, null, fan1.getUsername()},
      {null, null, fan2.getUsername()},
      {"java", null, fan1.getUsername()},
      {"java", author.getUsername(), null},
      {"spring", otherAuthor.getUsername(), null},
      {"missing", null, null}
    };
    for (String[] f : filters) {
      int listed = articleReadService.queryArticles(f[0], f[1], f[2], new Page(0, 100)).size();
      int counted = articleReadService.countArticle(f[0], f[1], f[2]);
      Assertions.assertEquals(listed, counted, "mismatch for filter " + Arrays.toString(f));
    }
  }

  @Test
  public void should_create_article_list_indexes() throws SQLException {
    Set<String> indexes = new HashSet<>();
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "select name, tbl_name from sqlite_master where type = 'index'");
        ResultSet rs = statement.executeQuery()) {
      while (rs.next()) {
        indexes.add(rs.getString("tbl_name") + "." + rs.getString("name"));
      }
    }
    Assertions.assertTrue(indexes.contains("articles.idx_articles_created_at"));
    Assertions.assertTrue(indexes.contains("article_tags.idx_article_tags_article_id"));
    Assertions.assertTrue(indexes.contains("article_tags.idx_article_tags_tag_id"));
  }
}
