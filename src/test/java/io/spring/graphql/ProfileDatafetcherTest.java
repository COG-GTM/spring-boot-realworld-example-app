package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.exceptions.QueryException;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.UserData;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class ProfileDatafetcherTest extends DgsGraphQLTestBase {

  @Test
  void should_query_profile() {
    setAnonymous();
    when(profileQueryService.findByUsername(eq("johnjacob"), any()))
        .thenReturn(Optional.of(profileData(user)));

    String username =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ profile(username: \"johnjacob\") { profile { username bio image following } } }",
            "data.profile.profile.username");

    assertEquals("johnjacob", username);
  }

  @Test
  void should_error_when_profile_not_found() {
    setAnonymous();
    when(profileQueryService.findByUsername(any(), any())).thenReturn(Optional.empty());

    QueryException error =
        org.junit.jupiter.api.Assertions.assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "{ profile(username: \"ghost\") { profile { username } } }",
                    "data.profile.profile"));

    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void should_resolve_article_author() {
    setAnonymous();
    ArticleData articleData = articleData("hello-world", user);
    when(articleQueryService.findBySlug(eq("hello-world"), any()))
        .thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData(user)));

    String authorName =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ article(slug: \"hello-world\") { slug author { username following } } }",
            "data.article.author.username");

    assertEquals(user.getUsername(), authorName);
  }

  @Test
  void should_resolve_comment_author() {
    setAnonymous();
    ArticleData articleData = articleData("hello-world", user);
    CommentData commentData = commentData("comment-1", user);
    when(articleQueryService.findBySlug(eq("hello-world"), any()))
        .thenReturn(Optional.of(articleData));
    when(commentQueryService.findByArticleIdWithCursor(any(), any(), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(commentData), Direction.NEXT, false));
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData(user)));

    java.util.List<String> authorNames =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ article(slug: \"hello-world\") { comments(first: 5) { edges { node { id author {"
                + " username } } } } } }",
            "data.article.comments.edges[*].node.author.username");

    assertEquals(Collections.singletonList(user.getUsername()), authorNames);
  }

  @Test
  void should_resolve_user_profile_via_me() {
    setAuthenticatedUser(user);
    UserData userData =
        new UserData(user.getId(), user.getEmail(), user.getUsername(), user.getBio(), "img");
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData(user)));

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token jwt-token");

    String username =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ me { profile { username } } }", "data.me.profile.username", headers);

    assertEquals(user.getUsername(), username);
  }
}
