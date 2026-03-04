package io.spring.e2e;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public class CommentE2ETest extends BaseE2ETest {

  @Test
  void should_create_comment_on_article() {
    String token = registerAndGetToken("commentauthor@test.com", "commentauthor", "password123");

    Map<String, Object> articleBody = articleParam("Comment Article", "Desc", "Body", new String[] {});
    post("/articles", articleBody, token);

    Map<String, Object> commentBody = commentParam("This is a great article!");
    ResponseEntity<String> response =
        post("/articles/comment-article/comments", commentBody, token);

    assertEquals(HttpStatus.CREATED.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    JsonNode comment = body.get("comment");
    assertEquals("This is a great article!", comment.get("body").asText());
    assertNotNull(comment.get("id"));
    assertNotNull(comment.get("createdAt"));
    assertEquals("commentauthor", comment.get("author").get("username").asText());
  }

  @Test
  void should_get_comments_for_article() {
    String token = registerAndGetToken("getcomments@test.com", "getcommentsuser", "password123");

    Map<String, Object> articleBody = articleParam("Comments List Article", "Desc", "Body", new String[] {});
    post("/articles", articleBody, token);

    Map<String, Object> comment1 = commentParam("First comment");
    post("/articles/comments-list-article/comments", comment1, token);

    Map<String, Object> comment2 = commentParam("Second comment");
    post("/articles/comments-list-article/comments", comment2, token);

    ResponseEntity<String> response = get("/articles/comments-list-article/comments");

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    assertTrue(body.has("comments"));
    assertTrue(body.get("comments").size() >= 2);
  }

  @Test
  void should_delete_own_comment() {
    String token = registerAndGetToken("delcomment@test.com", "delcommentuser", "password123");

    Map<String, Object> articleBody = articleParam("Delete Comment Article", "Desc", "Body", new String[] {});
    post("/articles", articleBody, token);

    Map<String, Object> commentBody = commentParam("Comment to delete");
    ResponseEntity<String> createResponse =
        post("/articles/delete-comment-article/comments", commentBody, token);

    JsonNode createBody = parseJson(createResponse.getBody());
    String commentId = createBody.get("comment").get("id").asText();

    ResponseEntity<String> deleteResponse =
        delete("/articles/delete-comment-article/comments/" + commentId, token);

    assertEquals(HttpStatus.NO_CONTENT.value(), deleteResponse.getStatusCodeValue());
  }

  @Test
  void should_fail_create_comment_without_auth() {
    String token =
        registerAndGetToken("noauthcomment@test.com", "noauthcommentuser", "password123");

    Map<String, Object> articleBody = articleParam("No Auth Comment Article", "Desc", "Body", new String[] {});
    post("/articles", articleBody, token);

    Map<String, Object> commentBody = commentParam("Unauthorized comment");
    HttpClientErrorException ex =
        expectClientError(
            HttpMethod.POST, "/articles/no-auth-comment-article/comments", commentBody, null);

    assertEquals(HttpStatus.UNAUTHORIZED.value(), ex.getRawStatusCode());
  }

  @Test
  void should_fail_delete_comment_by_non_author() {
    String token1 = registerAndGetToken("commentowner@test.com", "commentowneruser", "password123");
    String token2 = registerAndGetToken("commentother@test.com", "commentotheruser", "password123");

    Map<String, Object> articleBody = articleParam("Other Comment Article", "Desc", "Body", new String[] {});
    post("/articles", articleBody, token1);

    Map<String, Object> commentBody = commentParam("Owner comment");
    ResponseEntity<String> createResponse =
        post("/articles/other-comment-article/comments", commentBody, token1);

    JsonNode createBody = parseJson(createResponse.getBody());
    String commentId = createBody.get("comment").get("id").asText();

    HttpClientErrorException ex =
        expectClientError(
            HttpMethod.DELETE,
            "/articles/other-comment-article/comments/" + commentId,
            null,
            token2);

    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getRawStatusCode());
  }

  @Test
  void should_fail_create_comment_with_empty_body() {
    String token = registerAndGetToken("emptycomment@test.com", "emptycommentuser", "password123");

    Map<String, Object> articleBody = articleParam("Empty Comment Article", "Desc", "Body", new String[] {});
    post("/articles", articleBody, token);

    Map<String, Object> commentBody = commentParam("");
    HttpClientErrorException ex =
        expectClientError(
            HttpMethod.POST, "/articles/empty-comment-article/comments", commentBody, token);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getRawStatusCode());
  }
}
