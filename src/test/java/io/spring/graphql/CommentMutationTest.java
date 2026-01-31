package io.spring.graphql;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.application.CommentQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.application.data.UserData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
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
public class CommentMutationTest {

  @Autowired private MockMvc mvc;

  @MockBean private CommentQueryService commentQueryService;

  @MockBean private ProfileQueryService profileQueryService;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private CommentRepository commentRepository;

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
  public void should_create_comment_success() throws Exception {
    String slug = "how-to-train-your-dragon";
    String commentBody = "This is a great article!";

    Article article =
        new Article(
            "How to train your dragon",
            "Ever wonder how?",
            "You have to believe",
            Collections.emptyList(),
            "author-id");
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    ProfileData profileData =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    CommentData commentData =
        new CommentData(
            "comment-id", commentBody, article.getId(), new DateTime(), new DateTime(), profileData);
    when(commentQueryService.findById(any(), any())).thenReturn(Optional.of(commentData));
    when(profileQueryService.findByUsername(eq(username), any())).thenReturn(Optional.of(profileData));

    String mutation =
        "mutation { addComment(slug: \\\""
            + slug
            + "\\\", body: \\\""
            + commentBody
            + "\\\") { comment { id body author { username } } } }";

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.addComment.comment.body", equalTo(commentBody))
        .body("data.addComment.comment.author.username", equalTo(username));

    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void should_fail_create_comment_without_authentication() throws Exception {
    String slug = "how-to-train-your-dragon";
    String commentBody = "This is a great article!";

    String mutation =
        "mutation { addComment(slug: \\\""
            + slug
            + "\\\", body: \\\""
            + commentBody
            + "\\\") { comment { id body } } }";

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
  public void should_fail_create_comment_article_not_found() throws Exception {
    String slug = "nonexistent-article";
    String commentBody = "This is a great article!";

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.empty());

    String mutation =
        "mutation { addComment(slug: \\\""
            + slug
            + "\\\", body: \\\""
            + commentBody
            + "\\\") { comment { id body } } }";

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
  public void should_delete_comment_by_comment_author() throws Exception {
    String slug = "how-to-train-your-dragon";
    String commentId = "comment-id";

    Article article =
        new Article(
            "How to train your dragon",
            "Ever wonder how?",
            "You have to believe",
            Collections.emptyList(),
            "different-author-id");
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    Comment comment = new Comment("Comment body", user.getId(), article.getId());
    when(commentRepository.findById(eq(article.getId()), eq(commentId)))
        .thenReturn(Optional.of(comment));

    String mutation =
        "mutation { deleteComment(slug: \\\""
            + slug
            + "\\\", id: \\\""
            + commentId
            + "\\\") { success } }";

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.deleteComment.success", equalTo(true));

    verify(commentRepository).remove(any(Comment.class));
  }

  @Test
  public void should_delete_comment_by_article_author() throws Exception {
    String slug = "how-to-train-your-dragon";
    String commentId = "comment-id";

    Article article =
        new Article(
            "How to train your dragon",
            "Ever wonder how?",
            "You have to believe",
            Collections.emptyList(),
            user.getId());
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    Comment comment = new Comment("Comment body", "different-user-id", article.getId());
    when(commentRepository.findById(eq(article.getId()), eq(commentId)))
        .thenReturn(Optional.of(comment));

    String mutation =
        "mutation { deleteComment(slug: \\\""
            + slug
            + "\\\", id: \\\""
            + commentId
            + "\\\") { success } }";

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.deleteComment.success", equalTo(true));

    verify(commentRepository).remove(any(Comment.class));
  }

  @Test
  public void should_fail_delete_comment_unauthorized() throws Exception {
    String slug = "how-to-train-your-dragon";
    String commentId = "comment-id";

    Article article =
        new Article(
            "How to train your dragon",
            "Ever wonder how?",
            "You have to believe",
            Collections.emptyList(),
            "article-author-id");
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    Comment comment = new Comment("Comment body", "comment-author-id", article.getId());
    when(commentRepository.findById(eq(article.getId()), eq(commentId)))
        .thenReturn(Optional.of(comment));

    String mutation =
        "mutation { deleteComment(slug: \\\""
            + slug
            + "\\\", id: \\\""
            + commentId
            + "\\\") { success } }";

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
  public void should_fail_delete_comment_not_found() throws Exception {
    String slug = "how-to-train-your-dragon";
    String commentId = "nonexistent-comment-id";

    Article article =
        new Article(
            "How to train your dragon",
            "Ever wonder how?",
            "You have to believe",
            Collections.emptyList(),
            user.getId());
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    when(commentRepository.findById(eq(article.getId()), eq(commentId)))
        .thenReturn(Optional.empty());

    String mutation =
        "mutation { deleteComment(slug: \\\""
            + slug
            + "\\\", id: \\\""
            + commentId
            + "\\\") { success } }";

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
  public void should_fail_delete_comment_without_authentication() throws Exception {
    String slug = "how-to-train-your-dragon";
    String commentId = "comment-id";

    String mutation =
        "mutation { deleteComment(slug: \\\""
            + slug
            + "\\\", id: \\\""
            + commentId
            + "\\\") { success } }";

    given()
        .contentType("application/json")
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("errors[0].extensions.errorType", equalTo("INTERNAL"));
  }
}
