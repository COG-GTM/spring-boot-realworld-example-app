package io.spring.graphql;

import static org.mockito.Mockito.mock;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironment;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Shared fixtures and Spring-Security context helpers for the pure-Mockito GraphQL layer tests.
 * These tests never touch the database or a DGS context, so they are parallel-safe.
 */
abstract class GraphQLTestBase {

  protected User newUser() {
    return new User("john@jacob.com", "johnjacob", "123", "bio", "avatar.png");
  }

  protected void setCurrentUser(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  /** Mimics Spring Security's anonymous authentication when there is no logged-in user. */
  protected void setAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  /** DGS wraps a plain {@link DataFetchingEnvironment}; mock the inner one and stub it. */
  protected DataFetchingEnvironment mockEnv() {
    return mock(DataFetchingEnvironment.class);
  }

  protected DgsDataFetchingEnvironment dgs(DataFetchingEnvironment dfe) {
    return new DgsDataFetchingEnvironment(dfe);
  }

  protected ProfileData profileData(String username) {
    return new ProfileData("profile-id", username, "some bio", "image.png", false);
  }

  protected ArticleData articleData(String id, String slug, String authorUsername) {
    return new ArticleData(
        id,
        slug,
        "a title",
        "a description",
        "a body",
        false,
        0,
        new DateTime(),
        new DateTime(),
        Arrays.asList("java", "spring"),
        profileData(authorUsername));
  }

  protected CommentData commentData(String id, String articleId, String authorUsername) {
    return new CommentData(
        id, "a comment body", articleId, new DateTime(), new DateTime(), profileData(authorUsername));
  }
}
