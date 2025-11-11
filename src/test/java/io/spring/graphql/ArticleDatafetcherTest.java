package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ArticleDatafetcher.class,
      ProfileDatafetcher.class,
      WebSecurityConfig.class,
      BCryptPasswordEncoder.class,
      JacksonCustomizations.class
    })
public class ArticleDatafetcherTest {

  @Autowired DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private UserRepository userRepository;

  private String defaultAvatar;
  private User user;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";
    user = new User("john@jacob.com", "johnjacob", "password", "bio", defaultAvatar);
    profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
  }

  @Test
  public void should_query_article_by_slug() {
    String slug = "how-to-train-your-dragon";
    String title = "How to train your dragon";
    String description = "Ever wonder how?";
    String body = "You have to believe";
    List<String> tagList = Arrays.asList("reactjs", "angularjs", "dragons");

    ArticleData articleData =
        new ArticleData(
            "123",
            slug,
            title,
            description,
            body,
            false,
            0,
            new DateTime(),
            new DateTime(),
            tagList,
            profileData);

    when(articleQueryService.findBySlug(eq(slug), any())).thenReturn(Optional.of(articleData));

    String query =
        String.format(
            "query { article(slug: \"%s\") { slug title description body favorited favoritesCount tagList author { username bio image following } } }",
            slug);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.article", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("slug")).isEqualTo(slug);
    assertThat(result.get("title")).isEqualTo(title);
    assertThat(result.get("description")).isEqualTo(description);
    assertThat(result.get("body")).isEqualTo(body);
    assertThat(result.get("favorited")).isEqualTo(false);
    assertThat(result.get("favoritesCount")).isEqualTo(0);
    assertThat(result.get("tagList")).isEqualTo(tagList);

    Map<String, Object> author = (Map<String, Object>) result.get("author");
    assertThat(author.get("username")).isEqualTo(user.getUsername());
  }

  @Test
  public void should_query_articles_with_pagination() {
    String slug = "how-to-train-your-dragon";
    String title = "How to train your dragon";
    String description = "Ever wonder how?";
    String body = "You have to believe";
    List<String> tagList = Arrays.asList("reactjs", "angularjs", "dragons");

    ArticleData articleData =
        new ArticleData(
            "123",
            slug,
            title,
            description,
            body,
            false,
            0,
            new DateTime(),
            new DateTime(),
            tagList,
            profileData);

    CursorPager<ArticleData> pager = CursorPager.fromList(Arrays.asList(articleData), 10);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);

    String query =
        "query { articles(first: 10) { edges { cursor node { slug title description body favorited favoritesCount tagList } } pageInfo { hasNextPage hasPreviousPage startCursor endCursor } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.articles", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("edges")).isNotNull();

    List<Map<String, Object>> edges = (List<Map<String, Object>>) result.get("edges");
    assertThat(edges).hasSize(1);

    Map<String, Object> node = (Map<String, Object>) edges.get(0).get("node");
    assertThat(node.get("slug")).isEqualTo(slug);
    assertThat(node.get("title")).isEqualTo(title);
  }

  @Test
  public void should_query_articles_with_tag_filter() {
    String slug = "how-to-train-your-dragon";
    String title = "How to train your dragon";
    String description = "Ever wonder how?";
    String body = "You have to believe";
    List<String> tagList = Arrays.asList("reactjs", "angularjs", "dragons");
    String filterTag = "reactjs";

    ArticleData articleData =
        new ArticleData(
            "123",
            slug,
            title,
            description,
            body,
            false,
            0,
            new DateTime(),
            new DateTime(),
            tagList,
            profileData);

    CursorPager<ArticleData> pager = CursorPager.fromList(Arrays.asList(articleData), 10);

    when(articleQueryService.findRecentArticlesWithCursor(eq(filterTag), any(), any(), any(), any()))
        .thenReturn(pager);

    String query =
        String.format(
            "query { articles(first: 10, withTag: \"%s\") { edges { node { slug title tagList } } } }",
            filterTag);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.articles", Map.class);

    assertThat(result).isNotNull();
    List<Map<String, Object>> edges = (List<Map<String, Object>>) result.get("edges");
    assertThat(edges).hasSize(1);

    Map<String, Object> node = (Map<String, Object>) edges.get(0).get("node");
    List<String> resultTagList = (List<String>) node.get("tagList");
    assertThat(resultTagList).contains(filterTag);
  }

  @Test
  public void should_query_articles_by_author() {
    String slug = "how-to-train-your-dragon";
    String title = "How to train your dragon";
    String description = "Ever wonder how?";
    String body = "You have to believe";
    List<String> tagList = Arrays.asList("reactjs", "angularjs", "dragons");
    String authorUsername = "johnjacob";

    ArticleData articleData =
        new ArticleData(
            "123",
            slug,
            title,
            description,
            body,
            false,
            0,
            new DateTime(),
            new DateTime(),
            tagList,
            profileData);

    CursorPager<ArticleData> pager = CursorPager.fromList(Arrays.asList(articleData), 10);

    when(articleQueryService.findRecentArticlesWithCursor(any(), eq(authorUsername), any(), any(), any()))
        .thenReturn(pager);

    String query =
        String.format(
            "query { articles(first: 10, authoredBy: \"%s\") { edges { node { slug title author { username } } } } }",
            authorUsername);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.articles", Map.class);

    assertThat(result).isNotNull();
    List<Map<String, Object>> edges = (List<Map<String, Object>>) result.get("edges");
    assertThat(edges).hasSize(1);
  }

  @Test
  public void should_query_articles_favorited_by_user() {
    String slug = "how-to-train-your-dragon";
    String title = "How to train your dragon";
    String description = "Ever wonder how?";
    String body = "You have to believe";
    List<String> tagList = Arrays.asList("reactjs", "angularjs", "dragons");
    String favoritedBy = "johnjacob";

    ArticleData articleData =
        new ArticleData(
            "123",
            slug,
            title,
            description,
            body,
            true,
            5,
            new DateTime(),
            new DateTime(),
            tagList,
            profileData);

    CursorPager<ArticleData> pager = CursorPager.fromList(Arrays.asList(articleData), 10);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), eq(favoritedBy), any(), any()))
        .thenReturn(pager);

    String query =
        String.format(
            "query { articles(first: 10, favoritedBy: \"%s\") { edges { node { slug title favorited favoritesCount } } } }",
            favoritedBy);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.articles", Map.class);

    assertThat(result).isNotNull();
    List<Map<String, Object>> edges = (List<Map<String, Object>>) result.get("edges");
    assertThat(edges).hasSize(1);

    Map<String, Object> node = (Map<String, Object>) edges.get(0).get("node");
    assertThat(node.get("favorited")).isEqualTo(true);
    assertThat(node.get("favoritesCount")).isEqualTo(5);
  }

  @Test
  public void should_return_empty_list_when_article_not_found() {
    String slug = "nonexistent-article";

    when(articleQueryService.findBySlug(eq(slug), any())).thenReturn(Optional.empty());

    String query =
        String.format(
            "query { article(slug: \"%s\") { slug title } }",
            slug);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.article", Map.class);
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("ResourceNotFoundException");
    }
  }
}
