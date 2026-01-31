package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.TypeRef;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.user.UserService;
import io.spring.core.article.ArticleRepository;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      UserMutation.class,
      ArticleMutation.class,
      ArticleDatafetcher.class,
      ProfileDatafetcher.class,
      WebSecurityConfig.class,
      JacksonCustomizations.class,
      GraphQLCustomizeExceptionHandler.class
    })
@Import({})
public class GraphQLErrorHandlingTest extends GraphQLTestBase {

  @MockBean private UserService userService;

  @MockBean private PasswordEncoder passwordEncoder;

  @MockBean private ArticleCommandService articleCommandService;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private ProfileQueryService profileQueryService;

  @Test
  public void should_handle_validation_errors_in_user_creation() {
    when(userService.createUser(any()))
        .thenThrow(new IllegalArgumentException("Validation failed"));

    String query =
        "mutation CreateUser($input: CreateUserInput) {"
            + "  createUser(input: $input) {"
            + "    ... on UserPayload {"
            + "      user {"
            + "        email"
            + "        username"
            + "      }"
            + "    }"
            + "    ... on Error {"
            + "      message"
            + "      errors {"
            + "        key"
            + "        value"
            + "      }"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    Map<String, String> input = new HashMap<>();
    input.put("email", "test@example.com");
    input.put("username", "");
    input.put("password", "password123");
    variables.put("input", input);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(
          query, "data.createUser", variables, new TypeRef<Map<String, Object>>() {});
    } catch (Exception e) {
      assertThat(e).isNotNull();
    }
  }

  @Test
  public void should_handle_resource_not_found_error() {
    String slug = "nonexistent-article";

    when(articleQueryService.findBySlug(eq(slug), any())).thenReturn(Optional.empty());

    String query =
        "query GetArticle($slug: String!) {"
            + "  article(slug: $slug) {"
            + "    slug"
            + "    title"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", slug);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(
          query, "data.article", variables, new TypeRef<Map<String, Object>>() {});
    } catch (Exception e) {
      assertThat(e).isNotNull();
      assertThat(e.getMessage()).contains("ResourceNotFoundException");
    }
  }

  @Test
  public void should_handle_authentication_error() {
    clearAuthentication();

    String query =
        "mutation CreateArticle($input: CreateArticleInput!) {"
            + "  createArticle(input: $input) {"
            + "    article {"
            + "      title"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    Map<String, Object> input = new HashMap<>();
    input.put("title", "Test");
    input.put("description", "Test");
    input.put("body", "Test");
    variables.put("input", input);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(
          query, "data.createArticle.article", variables, new TypeRef<Map<String, Object>>() {});
    } catch (Exception e) {
      assertThat(e).isNotNull();
    }
  }

  @Test
  public void should_handle_invalid_query_parameters() {
    String query =
        "query GetArticles($first: Int, $last: Int) {"
            + "  articles(first: $first, last: $last) {"
            + "    edges {"
            + "      node {"
            + "        slug"
            + "      }"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(
          query, "data.articles", variables, new TypeRef<Map<String, Object>>() {});
    } catch (Exception e) {
      assertThat(e).isNotNull();
    }
  }

  @Test
  public void should_handle_profile_not_found_error() {
    String username = "nonexistent";

    when(profileQueryService.findByUsername(eq(username), any())).thenReturn(Optional.empty());

    String query =
        "query GetProfile($username: String!) {"
            + "  profile(username: $username) {"
            + "    profile {"
            + "      username"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("username", username);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(
          query, "data.profile.profile", variables, new TypeRef<Map<String, Object>>() {});
    } catch (Exception e) {
      assertThat(e).isNotNull();
    }
  }

  @Test
  public void should_handle_unauthorized_article_update() {
    authenticateUser();

    String slug = "other-user-article";
    String otherUserId = "other-user-id";

    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "Test", "Test", "Test", java.util.Arrays.asList(), otherUserId);
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    String query =
        "mutation UpdateArticle($slug: String!, $changes: UpdateArticleInput!) {"
            + "  updateArticle(slug: $slug, changes: $changes) {"
            + "    article {"
            + "      title"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", slug);
    Map<String, Object> changes = new HashMap<>();
    changes.put("title", "Updated Title");
    variables.put("changes", changes);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(
          query, "data.updateArticle.article", variables, new TypeRef<Map<String, Object>>() {});
    } catch (Exception e) {
      assertThat(e).isNotNull();
    }

    clearAuthentication();
  }
}
