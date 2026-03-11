package io.spring.integration;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class GraphQLIntegrationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @Autowired private UserRepository userRepository;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private User testUser;
  private Article testArticle;

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.clearContext();

    testUser =
        new User(
            "graphql@example.com",
            "graphqluser",
            passwordEncoder.encode("password"),
            "Test bio",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    userRepository.save(testUser);

    testArticle =
        new Article(
            "GraphQL Test Article",
            "Test Description",
            "Test Body",
            Arrays.asList("graphql", "java"),
            testUser.getId());
    articleRepository.save(testArticle);
  }

  private void authenticateUser(User user) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(user, null, List.of());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @Test
  public void should_execute_tags_query() {
    String query = "{ tags }";

    ExecutionResult result = dgsQueryExecutor.execute(query);

    assertThat(result, notNullValue());
    assertThat(result.getData(), notNullValue());
  }

  @Test
  public void should_execute_articles_query() {
    String query = "{ articles(first: 10) { edges { node { title slug } } } }";

    ExecutionResult result = dgsQueryExecutor.execute(query);

    assertThat(result, notNullValue());
  }

  @Test
  public void should_execute_article_query_by_slug() {
    String query =
        String.format(
            "{ article(slug: \"%s\") { title description body slug } }", testArticle.getSlug());

    ExecutionResult result = dgsQueryExecutor.execute(query);

    assertThat(result, notNullValue());
  }

  @Test
  public void should_execute_profile_query() {
    String query =
        String.format(
            "{ profile(username: \"%s\") { profile { username bio } } }", testUser.getUsername());

    ExecutionResult result = dgsQueryExecutor.execute(query);

    assertThat(result, notNullValue());
  }

  @Test
  public void should_execute_create_user_mutation() {
    String mutation =
        "mutation { createUser(input: { email: \"newuser@example.com\", username: \"newuser\", password: \"password123\" }) { ... on UserPayload { user { email username } } } }";

    ExecutionResult result = dgsQueryExecutor.execute(mutation);

    assertThat(result, notNullValue());
  }

  @Test
  public void should_execute_login_mutation() {
    String mutation =
        String.format(
            "mutation { login(email: \"%s\", password: \"password\") { user { email username token } } }",
            testUser.getEmail());

    ExecutionResult result = dgsQueryExecutor.execute(mutation);

    assertThat(result, notNullValue());
  }

  @Test
  public void should_execute_feed_query_when_authenticated() {
    authenticateUser(testUser);

    String query = "{ feed(first: 10) { edges { node { title } } } }";

    ExecutionResult result = dgsQueryExecutor.execute(query);

    assertThat(result, notNullValue());
  }

  @Test
  public void should_execute_me_query_when_authenticated() {
    authenticateUser(testUser);

    String query = "{ me { email username } }";

    ExecutionResult result = dgsQueryExecutor.execute(query);

    assertThat(result, notNullValue());
  }

  @Test
  public void should_execute_create_article_mutation_when_authenticated() {
    authenticateUser(testUser);

    String mutation =
        "mutation { createArticle(input: { title: \"New GraphQL Article\", description: \"New Description\", body: \"New Body\", tagList: [\"test\"] }) { article { title slug } } }";

    ExecutionResult result = dgsQueryExecutor.execute(mutation);

    assertThat(result, notNullValue());
  }

  @Test
  public void should_execute_update_article_mutation_when_authenticated() {
    authenticateUser(testUser);

    String mutation =
        String.format(
            "mutation { updateArticle(slug: \"%s\", changes: { title: \"Updated GraphQL Title\" }) { article { title } } }",
            testArticle.getSlug());

    ExecutionResult result = dgsQueryExecutor.execute(mutation);

    assertThat(result, notNullValue());
  }

  @Test
  public void should_execute_favorite_article_mutation_when_authenticated() {
    authenticateUser(testUser);

    String mutation =
        String.format(
            "mutation { favoriteArticle(slug: \"%s\") { article { favorited favoritesCount } } }",
            testArticle.getSlug());

    ExecutionResult result = dgsQueryExecutor.execute(mutation);

    assertThat(result, notNullValue());
  }

  @Test
  public void should_execute_unfavorite_article_mutation_when_authenticated() {
    authenticateUser(testUser);

    String mutation =
        String.format(
            "mutation { unfavoriteArticle(slug: \"%s\") { article { favorited } } }",
            testArticle.getSlug());

    ExecutionResult result = dgsQueryExecutor.execute(mutation);

    assertThat(result, notNullValue());
  }

  @Test
  public void should_execute_delete_article_mutation_when_authenticated() {
    authenticateUser(testUser);

    Article articleToDelete =
        new Article(
            "Article To Delete", "Description", "Body", Arrays.asList("delete"), testUser.getId());
    articleRepository.save(articleToDelete);

    String mutation =
        String.format(
            "mutation { deleteArticle(slug: \"%s\") { success } }", articleToDelete.getSlug());

    ExecutionResult result = dgsQueryExecutor.execute(mutation);

    assertThat(result, notNullValue());
  }

  @Test
  public void should_execute_update_user_mutation_when_authenticated() {
    authenticateUser(testUser);

    String mutation =
        "mutation { updateUser(changes: { bio: \"Updated bio via GraphQL\" }) { user { bio } } }";

    ExecutionResult result = dgsQueryExecutor.execute(mutation);

    assertThat(result, notNullValue());
  }

  @Test
  public void should_execute_follow_user_mutation_when_authenticated() {
    User userToFollow =
        new User(
            "tofollow@example.com",
            "usertofollow",
            passwordEncoder.encode("password"),
            "Bio",
            "image.jpg");
    userRepository.save(userToFollow);

    authenticateUser(testUser);

    String mutation =
        String.format(
            "mutation { followUser(username: \"%s\") { profile { username following } } }",
            userToFollow.getUsername());

    ExecutionResult result = dgsQueryExecutor.execute(mutation);

    assertThat(result, notNullValue());
  }

  @Test
  public void should_execute_unfollow_user_mutation_when_authenticated() {
    User userToUnfollow =
        new User(
            "tounfollow@example.com",
            "usertounfollow",
            passwordEncoder.encode("password"),
            "Bio",
            "image.jpg");
    userRepository.save(userToUnfollow);

    authenticateUser(testUser);

    String mutation =
        String.format(
            "mutation { unfollowUser(username: \"%s\") { profile { username following } } }",
            userToUnfollow.getUsername());

    ExecutionResult result = dgsQueryExecutor.execute(mutation);

    assertThat(result, notNullValue());
  }

  @Test
  public void should_execute_articles_by_author_query() {
    String query =
        String.format(
            "{ articles(authoredBy: \"%s\", first: 10) { edges { node { title } } } }",
            testUser.getUsername());

    ExecutionResult result = dgsQueryExecutor.execute(query);

    assertThat(result, notNullValue());
  }

  @Test
  public void should_execute_articles_by_tag_query() {
    String query = "{ articles(withTag: \"graphql\", first: 10) { edges { node { title } } } }";

    ExecutionResult result = dgsQueryExecutor.execute(query);

    assertThat(result, notNullValue());
  }
}
