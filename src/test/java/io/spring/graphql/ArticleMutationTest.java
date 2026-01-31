package io.spring.graphql;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.application.ArticleQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.application.data.UserData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class ArticleMutationTest {

  @Autowired private MockMvc mvc;

  @MockBean private ArticleCommandService articleCommandService;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private UserRepository userRepository;

  @MockBean private UserReadService userReadService;

  @MockBean private JwtService jwtService;

  protected User user;
  protected UserData userData;
  protected String token;
  protected String email;
  protected String username;
  protected String defaultAvatar;

  @BeforeEach
  public void setUp() throws Exception {
    RestAssuredMockMvc.mockMvc(mvc);
    userFixture();
  }

  protected void userFixture() {
    email = "john@jacob.com";
    username = "johnjacob";
    defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";

    user = new User(email, username, "123", "", defaultAvatar);
    when(userRepository.findByUsername(eq(username))).thenReturn(Optional.of(user));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    userData = new UserData(user.getId(), email, username, "", defaultAvatar);
    when(userReadService.findById(eq(user.getId()))).thenReturn(userData);

    token = "token";
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));
    when(jwtService.toToken(any())).thenReturn(token);
  }

  @Test
  public void should_create_article_success() throws Exception {
    String title = "How to train your dragon";
    String description = "Ever wonder how?";
    String body = "You have to believe";

    Article article = new Article(title, description, body, Collections.emptyList(), user.getId());
    when(articleCommandService.createArticle(any(), any())).thenReturn(article);

    ProfileData profileData =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    ArticleData articleData =
        new ArticleData(
            article.getId(),
            article.getSlug(),
            title,
            description,
            body,
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            profileData);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { createArticle(input: {title: \\\""
            + title
            + "\\\", description: \\\""
            + description
            + "\\\", body: \\\""
            + body
            + "\\\"}) { article { title description body slug } } }";

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.createArticle.article.title", equalTo(title))
        .body("data.createArticle.article.description", equalTo(description))
        .body("data.createArticle.article.body", equalTo(body));

    verify(articleCommandService).createArticle(any(), any());
  }

  @Test
  public void should_create_article_with_tags() throws Exception {
    String title = "How to train your dragon";
    String description = "Ever wonder how?";
    String body = "You have to believe";

    Article article =
        new Article(title, description, body, Arrays.asList("dragons", "training"), user.getId());
    when(articleCommandService.createArticle(any(), any())).thenReturn(article);

    ProfileData profileData =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    ArticleData articleData =
        new ArticleData(
            article.getId(),
            article.getSlug(),
            title,
            description,
            body,
            false,
            0,
            new DateTime(),
            new DateTime(),
            Arrays.asList("dragons", "training"),
            profileData);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { createArticle(input: {title: \\\""
            + title
            + "\\\", description: \\\""
            + description
            + "\\\", body: \\\""
            + body
            + "\\\", tagList: [\\\"dragons\\\", \\\"training\\\"]}) { article { title tagList } } }";

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.createArticle.article.title", equalTo(title));

    verify(articleCommandService).createArticle(any(), any());
  }

  @Test
  public void should_fail_create_article_without_authentication() throws Exception {
    String title = "How to train your dragon";
    String description = "Ever wonder how?";
    String body = "You have to believe";

    String mutation =
        "mutation { createArticle(input: {title: \\\""
            + title
            + "\\\", description: \\\""
            + description
            + "\\\", body: \\\""
            + body
            + "\\\"}) { article { title } } }";

    given()
        .contentType("application/json")
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("errors[0].extensions.errorType", equalTo("INTERNAL"));
  }

  @Test
  public void should_update_article_success() throws Exception {
    String slug = "how-to-train-your-dragon";
    String newTitle = "Updated title";
    String newDescription = "Updated description";
    String newBody = "Updated body";

    Article article =
        new Article("How to train your dragon", "Ever wonder how?", "You have to believe", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    Article updatedArticle =
        new Article(newTitle, newDescription, newBody, Collections.emptyList(), user.getId());
    when(articleCommandService.updateArticle(any(), any())).thenReturn(updatedArticle);

    ProfileData profileData =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    ArticleData articleData =
        new ArticleData(
            updatedArticle.getId(),
            updatedArticle.getSlug(),
            newTitle,
            newDescription,
            newBody,
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            profileData);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { updateArticle(slug: \\\""
            + slug
            + "\\\", changes: {title: \\\""
            + newTitle
            + "\\\", description: \\\""
            + newDescription
            + "\\\", body: \\\""
            + newBody
            + "\\\"}) { article { title description body } } }";

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.updateArticle.article.title", equalTo(newTitle))
        .body("data.updateArticle.article.description", equalTo(newDescription))
        .body("data.updateArticle.article.body", equalTo(newBody));
  }

  @Test
  public void should_fail_update_article_when_not_author() throws Exception {
    String slug = "how-to-train-your-dragon";
    String newTitle = "Updated title";

    Article article =
        new Article("How to train your dragon", "Ever wonder how?", "You have to believe", Collections.emptyList(), "different-user-id");
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    String mutation =
        "mutation { updateArticle(slug: \\\""
            + slug
            + "\\\", changes: {title: \\\""
            + newTitle
            + "\\\"}) { article { title } } }";

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("errors[0].extensions.errorType", equalTo("INTERNAL"));
  }

  @Test
  public void should_favorite_article_success() throws Exception {
    String slug = "how-to-train-your-dragon";

    Article article =
        new Article("How to train your dragon", "Ever wonder how?", "You have to believe", Collections.emptyList(), "author-id");
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    ProfileData profileData =
        new ProfileData("author-id", "author", "", "", false);
    ArticleData articleData =
        new ArticleData(
            article.getId(),
            article.getSlug(),
            "How to train your dragon",
            "Ever wonder how?",
            "You have to believe",
            true,
            1,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            profileData);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { favoriteArticle(slug: \\\"" + slug + "\\\") { article { title favorited } } }";

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.favoriteArticle.article.title", equalTo("How to train your dragon"));

    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  public void should_unfavorite_article_success() throws Exception {
    String slug = "how-to-train-your-dragon";

    Article article =
        new Article("How to train your dragon", "Ever wonder how?", "You have to believe", Collections.emptyList(), "author-id");
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    ArticleFavorite articleFavorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(articleFavorite));

    ProfileData profileData =
        new ProfileData("author-id", "author", "", "", false);
    ArticleData articleData =
        new ArticleData(
            article.getId(),
            article.getSlug(),
            "How to train your dragon",
            "Ever wonder how?",
            "You have to believe",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            profileData);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { unfavoriteArticle(slug: \\\"" + slug + "\\\") { article { title favorited } } }";

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.unfavoriteArticle.article.title", equalTo("How to train your dragon"));

    verify(articleFavoriteRepository).remove(any(ArticleFavorite.class));
  }

  @Test
  public void should_delete_article_success() throws Exception {
    String slug = "how-to-train-your-dragon";

    Article article =
        new Article("How to train your dragon", "Ever wonder how?", "You have to believe", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    String mutation = "mutation { deleteArticle(slug: \\\"" + slug + "\\\") { success } }";

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.deleteArticle.success", equalTo(true));

    verify(articleRepository).remove(any(Article.class));
  }

  @Test
  public void should_fail_delete_article_when_not_author() throws Exception {
    String slug = "how-to-train-your-dragon";

    Article article =
        new Article("How to train your dragon", "Ever wonder how?", "You have to believe", Collections.emptyList(), "different-user-id");
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    String mutation = "mutation { deleteArticle(slug: \\\"" + slug + "\\\") { success } }";

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("errors[0].extensions.errorType", equalTo("INTERNAL"));
  }

  @Test
  public void should_fail_delete_article_not_found() throws Exception {
    String slug = "nonexistent-article";

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.empty());

    String mutation = "mutation { deleteArticle(slug: \\\"" + slug + "\\\") { success } }";

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("errors[0].extensions.errorType", equalTo("INTERNAL"));
  }
}
